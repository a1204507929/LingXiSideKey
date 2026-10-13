package com.lingxikey.custom.xposed

import android.view.KeyEvent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.lingxikey.custom.model.ActionType
import com.lingxikey.custom.model.AppProfile
import com.lingxikey.custom.model.AppSettings
import com.lingxikey.custom.model.Gesture
import com.lingxikey.custom.model.CursorControlMode
import com.lingxikey.custom.model.CursorLongPressAction
import com.lingxikey.custom.model.OperationMode
import com.lingxikey.custom.model.SIDE_KEY_CODE
import com.lingxikey.custom.onXposedFailure
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

/** Log tag shared by every hook in this module. */
internal const val TAG = "LingXiKeyCustom"

/** OPPO/ColorOS camera package that hosts the shutter hooks. */
internal const val CAMERA_PACKAGE = "com.oplus.camera"

/** Xposed entry point for the framework key hook and shortcut editor. */
class XposedEntry : XposedModule() {
    override fun onModuleLoaded(param: ModuleLoadedParam) {
        XposedRuntime.bind(this)
        SettingsReader.bind(this)
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        XposedRuntime.bind(this)
        SettingsReader.bind(this)
        LegacyImeCursorHook.install()
        SideKeyModule.install(PackageHookParam("android", param.classLoader))
    }

    override fun onPackageReady(param: PackageReadyParam) {
        XposedRuntime.bind(this)
        SettingsReader.bind(this)
        if (param.packageName == "com.coloros.shortcuts") {
            ShortcutsHook.install(PackageHookParam(param.packageName, param.classLoader))
        }
        if (param.packageName == CAMERA_PACKAGE) {
            // The application object is created during bindApplication, which can happen after
            // onPackageReady. Retry on the main looper until a Context is available, otherwise
            // the receiver is never registered and shutter broadcasts go nowhere.
            installCameraBridgeWhenReady(param.classLoader)
        }
    }

    private fun installCameraBridgeWhenReady(classLoader: ClassLoader, attempt: Int = 0) {
        val context = ContextResolver.applicationContext()
        if (context == null && attempt < 40) {
            Handler(Looper.getMainLooper()).postDelayed({
                installCameraBridgeWhenReady(classLoader, attempt + 1)
            }, 250L)
            return
        }
        if (context == null) {
            XposedBridge.log("$TAG: camera process has no application context, bridge not installed")
            return
        }
        CameraShutterBridge.install(context, classLoader)
    }
}

/** Fallback for ROMs where the focused InputMethodService receives volume keys directly. */
private object LegacyImeCursorHook {
    private var installed = false
    private val lock = Any()
    private var callbackHandler: Handler? = null
    private data class Press(
        val keyCode: Int,
        val mode: CursorControlMode,
        val action: CursorLongPressAction,
        val repeatIntervalMs: Long,
        val service: InputMethodService,
        val handler: Handler,
        var longRunnable: Runnable? = null,
        var repeatRunnable: Runnable? = null
    )
    private val presses = mutableMapOf<Int, Press>()

    fun install() {
        if (installed) return
        installed = true
        runCatching {
            XposedHelpers.hookMethod(
                "android.inputmethodservice.InputMethodService", null, "onKeyDown",
                Int::class.javaPrimitiveType!!, KeyEvent::class.java,
                object : Hooker() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val keyCode = param.args.getOrNull(0) as? Int ?: return
                        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return
                        val event = param.args.getOrNull(1) as? KeyEvent ?: return
                        val settings = SettingsReader.load()
                        val mode = settings.cursorControlMode
                        if (!settings.enabled) return
                        if (mode == CursorControlMode.DISABLED) return
                        val service = param.thisObject as? InputMethodService ?: return
                        if (!runCatching { service.isInputViewShown }
                                .onXposedFailure("read InputMethodService input view visibility")
                                .getOrDefault(false)) return
                        if (event.repeatCount == 0) {
                            startPress(keyCode, mode, settings, service)
                        }
                        param.setResult(true)
                    }
                }
            )
            XposedHelpers.hookMethod(
                "android.inputmethodservice.InputMethodService", null, "onKeyUp",
                Int::class.javaPrimitiveType!!, KeyEvent::class.java,
                object : Hooker() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val keyCode = param.args.getOrNull(0) as? Int ?: return
                        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return
                        val handled = finishPress(keyCode)
                        val settings = SettingsReader.load()
                        val mode = settings.cursorControlMode
                        if (!settings.enabled) return
                        if (mode == CursorControlMode.DISABLED) return
                        val service = param.thisObject as? InputMethodService ?: return
                        if (handled && runCatching { service.isInputViewShown }
                                .onXposedFailure("read InputMethodService input view visibility")
                                .getOrDefault(false)) {
                            param.setResult(true)
                        }
                    }
                }
            )
            XposedBridge.log("LingXiKeyCustom: installed InputMethodService cursor fallback")
        }.onXposedFailure("install InputMethodService cursor fallback")
    }

    private fun cursorKeyCode(volumeKeyCode: Int, mode: CursorControlMode): Int {
        val volumeUpMovesLeft = mode == CursorControlMode.VOLUME_UP_LEFT
        return if (volumeKeyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (volumeUpMovesLeft) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        } else {
            if (volumeUpMovesLeft) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        }
    }

    private fun startPress(
        keyCode: Int,
        mode: CursorControlMode,
        settings: AppSettings,
        service: InputMethodService
    ) {
        val handler = getCallbackHandler(service)
        val press = Press(
            keyCode = keyCode,
            mode = mode,
            action = settings.cursorLongPressAction,
            repeatIntervalMs = settings.cursorRepeatIntervalMs,
            service = service,
            handler = handler
        )
        synchronized(lock) {
            cancelPressLocked(keyCode)
            presses[keyCode] = press
            service.sendDownUpKeyEvents(cursorKeyCode(keyCode, mode))
            val longRunnable = Runnable { triggerLongPress(press) }
            press.longRunnable = longRunnable
            press.handler.postDelayed(longRunnable, settings.cursorLongPressMs)
        }
    }

    private fun triggerLongPress(press: Press) {
        synchronized(lock) {
            if (presses[press.keyCode] !== press) return
            when (press.action) {
                CursorLongPressAction.NONE -> Unit
                CursorLongPressAction.REPEAT -> {
                    val repeatRunnable = object : Runnable {
                        override fun run() {
                            synchronized(lock) {
                                if (presses[press.keyCode] !== press) return
                                press.service.sendDownUpKeyEvents(cursorKeyCode(press.keyCode, press.mode))
                                press.handler.postDelayed(this, press.repeatIntervalMs)
                            }
                        }
                    }
                    press.repeatRunnable = repeatRunnable
                    press.handler.postDelayed(repeatRunnable, press.repeatIntervalMs)
                }
                CursorLongPressAction.EDGE -> {
                    press.service.sendDownUpKeyEvents(edgeKeyCode(press.keyCode, press.mode))
                }
            }
        }
    }

    private fun finishPress(keyCode: Int): Boolean {
        synchronized(lock) {
            val press = presses.remove(keyCode) ?: return false
            cancelPressLocked(press)
            return true
        }
    }

    private fun cancelPressLocked(keyCode: Int) {
        presses.remove(keyCode)?.let(::cancelPressLocked)
    }

    private fun cancelPressLocked(press: Press) {
        press.longRunnable?.let(press.handler::removeCallbacks)
        press.repeatRunnable?.let(press.handler::removeCallbacks)
    }

    private fun getCallbackHandler(service: InputMethodService): Handler = synchronized(lock) {
        callbackHandler ?: Handler(service.mainLooper).also {
            callbackHandler = it
        }
    }

    private fun edgeKeyCode(volumeKeyCode: Int, mode: CursorControlMode): Int {
        val moveLeft = if (volumeKeyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mode == CursorControlMode.VOLUME_UP_LEFT
        } else {
            mode == CursorControlMode.VOLUME_UP_RIGHT
        }
        return if (moveLeft) KeyEvent.KEYCODE_MOVE_HOME else KeyEvent.KEYCODE_MOVE_END
    }
}

internal object SideKeyModule {
    private var installed = false

    fun install(lpparam: PackageHookParam) {
        if (installed) return
        installed = true

        val controller = SideKeyController()
        val actionExecutor = ActionExecutor()
        // OPPO LingXi side key: routed through InputKeyEventNotifyService, not
        // StrategyActionButtonKeyLaunchApp (which only handles keycodes 765-772/780-782).
        val lingXiHookInstalled = installLingXiServiceHook(lpparam, controller, actionExecutor)
        // The Oplus strategy / PWM hooks are for OnePlus-style action buttons; on LingXi
        // devices they would double-handle the same key, so only install them as fallback.
        val oplusHookInstalled = if (lingXiHookInstalled) false else installOplusStrategyHook(lpparam, controller, actionExecutor)
        val phoneHookInstalled = installPhoneWindowHook(
            lpparam,
            controller,
            actionExecutor,
            handleSideKey = !oplusHookInstalled && !lingXiHookInstalled
        )
        // Register cursor handling after combination handling so configured media chords win over
        // the optional volume-key cursor feature.
        CursorControlController().install(lpparam)
        if (!oplusHookInstalled && !phoneHookInstalled && !lingXiHookInstalled) {
            XposedBridge.log("$TAG: framework key hook unavailable, starting raw input fallback")
            RawInputEventReader.start(controller, actionExecutor)
        }
    }

    /**
     * Hooks ColorOS's InputKeyEventNotifyService dispatcher. The LingXi side key emits
     * keyCode 672 (KEY_MACRO17) on /dev/input/event2. Returning 0 from
     * interceptKeyBeforeQueueing means ACTION_QUEUEING_NOT_PASS_TO_USER (consume).
     * When the system LingXi game mode (lingxi_need_launch_notice == 6) is active we
     * pass events through so the stock in-game mapping keeps working.
     */
    private fun installLingXiServiceHook(
        lpparam: PackageHookParam,
        controller: SideKeyController,
        executor: ActionExecutor
    ): Boolean {
        var installedAny = false
        // Primary: the concrete interceptor instance ColorOS actually invokes
        // (StrategyLingXiKey registers an anonymous KeyInterceptor subclass).
        installedAny = try {
            val interceptorClass = XposedHelpers.findClass(
                "com.android.server.policy.StrategyLingXiKey\$1",
                lpparam.classLoader
            )
            XposedHelpers.hookAllMethods(
                interceptorClass,
                "interceptKeyBeforeQueueing",
                object : Hooker() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val event = param.args[0] as? KeyEvent ?: return
                        XposedBridge.log("$TAG: lingxi-icpt keyCode=${event.keyCode} scan=${event.scanCode} action=${event.action} rep=${event.repeatCount}")
                        handleLingXiEvent(param, event, controller, executor)
                    }
                }
            )
            XposedBridge.log("$TAG: hooked StrategyLingXiKey\$1.interceptKeyBeforeQueueing")
            true
        } catch (error: Throwable) {
            XposedBridge.log("$TAG: LingXi interceptor hook unavailable: ${error.javaClass.simpleName}: ${error.message}")
            false
        }
        // Secondary diagnostic: the vendor dispatcher.
        val secondary = try {
            val serviceClass = XposedHelpers.findClass(
                "com.android.server.policy.InputKeyEventNotifyService",
                lpparam.classLoader
            )
            XposedHelpers.hookMethod(
                serviceClass,
                "interceptKeyBeforeQueueing",
                KeyEvent::class.java,
                Boolean::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                object : Hooker() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val keyCode = param.args[2] as? Int ?: return
                        val event = param.args[0] as? KeyEvent
                        XposedBridge.log("$TAG: lingxi-dispatch keyCode=$keyCode action=${event?.action} scan=${event?.scanCode} dev=${event?.deviceId}")
                        val settings = SettingsReader.load()
                        if (!settings.enabled || settings.operationMode == OperationMode.DISABLED) {
                            controller.cancel()
                            return
                        }
                        if (keyCode != SIDE_KEY_CODE) return
                        if (event == null) return
                        if (event.repeatCount > 0) {
                            param.setResult(0)
                            return
                        }
                        if (isLingXiGameMode()) {
                            // Keep stock game behaviour untouched.
                            controller.cancel()
                            return
                        }
                        val down = (param.args[1] as? Boolean) ?: (event.action == KeyEvent.ACTION_DOWN)
                        val interactive = (param.args[4] as? Boolean) ?: true
                        executor.updateContext(resolveSystemContext())
                        if (down) {
                            XposedBridge.log("$TAG: lingxi key down keyCode=$keyCode scanCode=${event.scanCode} interactive=$interactive")
                            controller.onDown(settings, executor, interactive)
                        } else {
                            XposedBridge.log("$TAG: lingxi key up keyCode=$keyCode scanCode=${event.scanCode} interactive=$interactive")
                            controller.onUp(settings, executor, interactive)
                        }
                        // Consume: ACTION_QUEUEING_NOT_PASS_TO_USER.
                        param.setResult(0)
                    }
                }
            )
            XposedBridge.log("$TAG: hooked InputKeyEventNotifyService.interceptKeyBeforeQueueing (LingXi key)")
            true
        } catch (error: Throwable) {
            XposedBridge.log("$TAG: LingXi service hook unavailable: ${error.javaClass.simpleName}: ${error.message}")
            false
        }
        return installedAny || secondary
    }

    /** Shared handling for LingXi key events. Firmware already classifies gestures:
     * 670 = SHORT_TAP confirm (single), 671 = DOUBLE_TAP confirm, 672 = physical press
     * (held long for long-press, no confirm event). Map them directly to actions. */
    private val lingXiHandler = Handler(Looper.getMainLooper())
    private var lingXiLongRunnable: Runnable? = null
    private var lingXiSuppressConfirmsUntil = 0L

    private fun handleLingXiEvent(
        param: Hooker.HookParam,
        event: KeyEvent,
        controller: SideKeyController,
        executor: ActionExecutor
    ) {
        val settings = SettingsReader.load()
        if (!settings.enabled || settings.operationMode == OperationMode.DISABLED) {
            cancelLingXiPending()
            return
        }
        val code = event.keyCode
        if (code != 670 && code != 671 && code != 672 && code != SIDE_KEY_CODE) return
        if (isLingXiGameMode()) {
            // Keep stock game behaviour untouched: let the event pass through.
            cancelLingXiPending()
            return
        }
        if (event.repeatCount > 0) {
            param.setResult(0)
            return
        }
        val interactive = param.args.getOrNull(4) as? Boolean ?: true
        executor.updateContext(resolveSystemContext())
        // Foreground package may override the global actions for this app.
        val profile = findForegroundProfile(settings)
        when {
            code == 672 && event.action == KeyEvent.ACTION_DOWN -> {
                lingXiActiveProfile = profile
                lingXiLongTriggered = false
                val suppressAt = SystemClock.uptimeMillis() + 1500L
                val runnable = Runnable {
                    lingXiLongTriggered = true
                    lingXiSuppressConfirmsUntil = suppressAt
                    XposedBridge.log("$TAG: lingxi long press triggered")
                    dispatchGesture(Gesture.LONG, settings, lingXiActiveProfile, executor, interactive)
                }
                lingXiLongRunnable = runnable
                lingXiHandler.postDelayed(runnable, lingXiLongPressMs)
            }
            code == 672 -> {
                lingXiLongRunnable?.let { lingXiHandler.removeCallbacks(it) }
                lingXiLongRunnable = null
            }
            code == 670 && event.action == KeyEvent.ACTION_UP -> {
                if (SystemClock.uptimeMillis() >= lingXiSuppressConfirmsUntil) {
                    XposedBridge.log("$TAG: lingxi single tap")
                    dispatchGesture(Gesture.SINGLE, settings, lingXiActiveProfile, executor, interactive)
                }
            }
            code == 671 && event.action == KeyEvent.ACTION_UP -> {
                if (SystemClock.uptimeMillis() >= lingXiSuppressConfirmsUntil) {
                    XposedBridge.log("$TAG: lingxi double tap")
                    dispatchGesture(Gesture.DOUBLE, settings, lingXiActiveProfile, executor, interactive)
                }
            }
        }
        // Consume all LingXi keycodes so ColorOS doesn't run its own shortcut.
        param.setResult(0)
    }

    private var lingXiActiveProfile: AppProfile? = null

    private fun sendCameraBroadcast(action: String) {
        runCatching {
            val context = resolveSystemContext() ?: return
            val intent = android.content.Intent(action)
                .setPackage(CAMERA_PACKAGE)
                .addFlags(android.content.Intent.FLAG_RECEIVER_FOREGROUND)
            context.sendBroadcast(intent)
            XposedBridge.log("$TAG: sent camera broadcast $action")
        }.onXposedFailure("send camera broadcast")
    }

    /** Executes the app-specific action when the foreground app has one, else the global action. */
    private fun dispatchGesture(
        gesture: Gesture,
        settings: AppSettings,
        profile: AppProfile?,
        executor: ActionExecutor,
        interactive: Boolean
    ) {
        val override = profile?.actionFor(gesture)
        if (profile != null && override != null && override != ActionType.NONE) {
            // Safety: only honour the profile when its app really is in the foreground. The profile
            // is captured at key-down, and the app may have changed by the time a tap is confirmed.
            val foreground = readForegroundPackage()
            if (foreground != profile.packageName) {
                XposedBridge.log("$TAG: profile ${profile.packageName} no longer foreground ($foreground), skipping")
                return
            }
            XposedBridge.log("$TAG: app override ${profile.packageName} ${gesture.name} -> $override")
            if (profile.packageName == CAMERA_PACKAGE && isCameraAction(override)) {
                sendCameraGesture(gesture, executor, settings, interactive, override)
                return
            }
            executor.execute(
                override,
                profile.customFor(gesture),
                settings,
                interactive,
                wakeScreen = false,
                vibrationEnabled = settings.vibrationEnabled
            )
            return
        }
        if (profile != null && override == ActionType.NONE) return
        val action = when (gesture) {
            Gesture.SINGLE -> settings.singleAction
            Gesture.DOUBLE -> settings.doubleAction
            Gesture.LONG -> settings.longAction
        }
        val custom = when (gesture) {
            Gesture.SINGLE -> settings.singleCustom
            Gesture.DOUBLE -> settings.doubleCustom
            Gesture.LONG -> settings.longCustom
        }
        val wake = when (gesture) {
            Gesture.SINGLE -> settings.singleWakeScreen
            Gesture.DOUBLE -> settings.doubleWakeScreen
            Gesture.LONG -> settings.longWakeScreen
        }
        val vibrate = settings.vibrationEnabled || when (gesture) {
            Gesture.SINGLE -> settings.singleVibrationEnabled
            Gesture.DOUBLE -> settings.doubleVibrationEnabled
            Gesture.LONG -> settings.longVibrationEnabled
        }
        if (action == ActionType.NONE) return
        // App-scoped actions (camera shutter, stopwatch, ...) must be configured under
        // "按应用配置" for their own app. If one was assigned to a global gesture, ignore it.
        val owner = ownerOfAppAction(action)
        if (owner != null) {
            XposedBridge.log("$TAG: ignoring app-scoped action $action set on a global gesture (configure it under 按应用配置 instead)")
            return
        }
        // Global "open app" style actions: if the target app is already in the foreground,
        // do nothing instead of re-launching it (which looked like the app reopening).
        executor.execute(action, custom, settings, interactive, wake, vibrate)
    }

    private fun isCameraAction(action: ActionType): Boolean =
        action == ActionType.CAMERA_SHUTTER

    /**
     * App-scoped actions only make sense inside their own app. Returns the package that owns the
     * action, or null for global actions. They must be configured in "按应用配置" (an AppProfile)
     * to run at all; if one was assigned to a global gesture it is ignored.
     */
    private fun ownerOfAppAction(action: ActionType): String? = when (action) {
        ActionType.CAMERA_SHUTTER -> CAMERA_PACKAGE

        ActionType.MUSIC_PLAY_PAUSE,
        ActionType.MUSIC_NEXT,
        ActionType.MUSIC_PREVIOUS,
        ActionType.VIDEO_SEEK_FORWARD,
        ActionType.VIDEO_SEEK_BACKWARD -> null // media keys work in any music/video app

        else -> null
    }

    /** Camera semantics: a tap captures a photo in photo mode, or starts/pauses recording in video mode. */
    private fun sendCameraGesture(
        gesture: Gesture,
        executor: ActionExecutor,
        settings: AppSettings,
        interactive: Boolean,
        action: ActionType
    ) {
        // The camera process decides internally: in photo mode a tap captures, in video mode a tap
        // starts or pauses recording. Long-press semantics were removed per user request.
        // 触感统一在按下时给出（vibratePressCue），这里不再重复振动。
        sendCameraBroadcast(CameraShutterBridge.ACTION_PRESS)
        XposedBridge.log("$TAG: camera gesture=$gesture action=$action -> ${CameraShutterBridge.ACTION_PRESS}")
    }

    /** Returns the enabled profile whose package is currently in the foreground, if any. */
    private fun findForegroundProfile(settings: AppSettings): AppProfile? {
        if (settings.appProfiles.isEmpty()) return null
        val foreground = readForegroundPackage() ?: return null
        return settings.appProfiles.firstOrNull { it.enabled && it.packageName == foreground }
    }

    private fun readForegroundPackage(): String? {
        val context = resolveSystemContext() ?: return null
        return try {
            // UsageStatsManager reflects the last foreground activity without root-only APIs.
            val usm = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE)
                as? android.app.usage.UsageStatsManager ?: return null
            val events = usm.queryEvents(System.currentTimeMillis() - 60000L, System.currentTimeMillis())
            var lastPackage: String? = null
            val ev = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    ev.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    lastPackage = ev.packageName
                }
            }
            lastPackage
        } catch (error: Throwable) {
            XposedBridge.log("$TAG: read foreground package failed: ${error.message}")
            null
        }
    }

    private fun cancelLingXiPending() {
        lingXiLongRunnable?.let { lingXiHandler.removeCallbacks(it) }
        lingXiLongRunnable = null
    }

    private var lingXiLongTriggered = false
    private var lingXiLongPressMs = 500L

    private var cachedSystemContext: android.content.Context? = null

    private fun resolveSystemContext(): android.content.Context? {
        cachedSystemContext?.let { return it }
        val context = ContextResolver.systemContext()
        if (context == null) {
            XposedBridge.log("$TAG: resolve system context failed (ActivityThread not attached)")
        } else {
            cachedSystemContext = context
        }
        return context
    }

    /** 6 = game mode (LINGXI_GAME_MODE_ID); other values: 5 desk, 7 notice/default. */
    private fun isLingXiGameMode(): Boolean {
        val context = resolveSystemContext() ?: return false
        return try {
            android.provider.Settings.System.getInt(
                context.contentResolver, "lingxi_need_launch_notice", 7
            ) == 6
        } catch (_: Throwable) {
            false
        }
    }

    private fun installOplusStrategyHook(
        lpparam: PackageHookParam,
        controller: SideKeyController,
        executor: ActionExecutor
    ): Boolean {
        return try {
            val strategyClass = XposedHelpers.findClass(
                "com.android.server.policy.StrategyActionButtonKeyLaunchApp",
                lpparam.classLoader
            )
            XposedHelpers.hookMethod(
                strategyClass,
                "actionInterceptKeyBeforeQueueing",
                KeyEvent::class.java,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                object : Hooker() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val event = param.args[0] as? KeyEvent ?: return
                        val settings = SettingsReader.load()
                        if (!settings.enabled || settings.operationMode == OperationMode.DISABLED) {
                            controller.cancel()
                            return
                        }
                        if (!SettingsReader.matches(event, settings)) return

                        val down = (param.args[3] as? Boolean) ?: (event.action == KeyEvent.ACTION_DOWN)
                        val interactive = (param.args[4] as? Boolean) ?: true
                        executor.updateStrategy(param.thisObject)
                        executor.updateContext(resolveContext(param.thisObject))
                        if (event.action == KeyEvent.ACTION_DOWN && down) {
                            XposedBridge.log("$TAG: side key down keyCode=${event.keyCode} scanCode=${event.scanCode} interactive=$interactive")
                            controller.onDown(settings, executor, interactive)
                            // Prevent ColorOS from launching its original shortcut as well.
                            param.setResult(if ((param.method as java.lang.reflect.Method).returnType == Int::class.javaPrimitiveType) 0 else null)
                        } else if (event.action == KeyEvent.ACTION_UP && !down) {
                            XposedBridge.log("$TAG: side key up keyCode=${event.keyCode} scanCode=${event.scanCode} interactive=$interactive")
                            controller.onUp(settings, executor, interactive)
                            param.setResult(if ((param.method as java.lang.reflect.Method).returnType == Int::class.javaPrimitiveType) 0 else null)
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: hooked Oplus StrategyActionButtonKeyLaunchApp")
            true
        } catch (error: Throwable) {
            XposedBridge.log("$TAG: Oplus strategy hook unavailable: ${error.javaClass.simpleName}: ${error.message}")
            false
        }
    }

    private fun installPhoneWindowHook(
        lpparam: PackageHookParam,
        controller: SideKeyController,
        executor: ActionExecutor,
        handleSideKey: Boolean
    ): Boolean {
        val classNames = listOf(
            "com.android.server.policy.PhoneWindowManager",
            "com.android.server.policy.PhoneWindowManagerExt"
        )
        for (className in classNames) {
            try {
                XposedHelpers.hookMethod(
                    className,
                    lpparam.classLoader,
                    "interceptKeyBeforeQueueing",
                    KeyEvent::class.java,
                    Int::class.javaPrimitiveType!!,
                    object : Hooker() {
                        override fun beforeHookedMethod(param: HookParam) {
                            val event = param.args[0] as? KeyEvent ?: return
                            val settings = SettingsReader.load()
                            if (!settings.enabled || settings.operationMode == OperationMode.DISABLED) {
                                controller.cancel()
                                return
                            }
                            if (controller.onAuxiliaryKeyEvent(event, executor)) {
                                param.setResult(0)
                                return
                            }
                            if (!handleSideKey) return
                            if (!SettingsReader.matches(event, settings)) return
                            val context = runCatching {
                                XposedHelpers.getObjectField(param.thisObject, "mContext") as? android.content.Context
                            }.onXposedFailure("read policy context").getOrNull()
                            executor.updateContext(context)
                            when (event.action) {
                                KeyEvent.ACTION_DOWN -> controller.onDown(settings, executor, true)
                                KeyEvent.ACTION_UP -> controller.onUp(settings, executor, true)
                            }
                            // Consume the matching key only while custom handling is enabled.
                            param.setResult(0)
                        }
                    }
                )
                XposedBridge.log("$TAG: hooked $className.interceptKeyBeforeQueueing")
                return true
            } catch (error: Throwable) {
                XposedBridge.log("$TAG: cannot hook $className: ${error.javaClass.simpleName}: ${error.message}")
            }
        }
        return false
    }

    private fun resolveContext(instance: Any): android.content.Context? = runCatching {
        XposedHelpers.getObjectField(instance, "mContext") as? android.content.Context
    }.onXposedFailure("resolve hooked instance context").getOrNull() ?: runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        activityThread.getMethod("currentApplication").invoke(null) as? android.content.Context
    }.onXposedFailure("resolve application context").getOrNull()
}
