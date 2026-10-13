package com.lingxikey.custom.xposed

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.app.ActivityOptions
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.view.KeyEvent
import com.lingxikey.custom.model.ActionType
import com.lingxikey.custom.model.AppSettings
import com.lingxikey.custom.model.CommonAction
import com.lingxikey.custom.model.CustomActionSettings
import com.lingxikey.custom.model.DEFAULT_UNKNOWN_MORSE_TOAST
import com.lingxikey.custom.model.SideKeyCombinationAction
import com.lingxikey.custom.onXposedFailure
import java.util.function.Consumer

/** KEYCODE_VIDEO_CAMERA (0x4F) has no public constant in the SDK. */
private const val KEYCODE_VIDEO_CAMERA = 79

/** Intent.CATEGORY_APP_CAMERA ("android.intent.category.APP_CAMERA"), a hidden constant. */
private const val CATEGORY_APP_CAMERA = "android.intent.category.APP_CAMERA"

/** Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND, a hidden framework flag needed to wake background apps. */
private const val FLAG_RECEIVER_INCLUDE_BACKGROUND = 0x01000000

class ActionExecutor {
    private companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val FLASHLIGHT_SERVICE =
            "com.oplus.systemui.statusbar.notification.keymagicservice.KeyFlashlightService"
        private const val DND_SERVICE =
            "com.oplus.systemui.statusbar.notification.keymagicservice.KeyDndService"
        private const val SEEDLING_ACTION = "com.oplus.seedlingservice.action.SEEDLING_SERVICE"
        private const val RECORDING_ACTION = "oplus.intent.action.START_RECORD_FROM_CUBE_BUTTON"
        private const val SMALL_WINDOW_MODE = 100
        /** KEYCODE_ASSIST 长按注入时 DOWN→UP 的保持时长，超过系统长按阈值。 */
        private const val ASSIST_LONG_PRESS_MS = 800L
        /** 长按持续振动的兜底时长（松手会立刻 cancel）。 */
        private const val LONG_PRESS_HAPTIC_MS = 30_000L
        /** 原厂风扇控制键（Settings.System）：main_switch 是状态键，modify 是控制信号键。 */
        private const val FAN_MAIN_SWITCH = "cooling_fan_main_switch"
        private const val FAN_MAIN_SWITCH_MODIFY = "cooling_fan_main_switch_modify"
        /**
         * 机身灯效（风扇氛围灯）亮度节点：0=灭，255=最亮。
         *
         * 与风扇 state 同属 oplus_fan_sysfs 标签，system_server 直写同样被 SELinux 拒绝，
         * 因此复用 root 守护进程信令：另开一个触发文件。
         */
    private const val FAN_LIGHT_PATH = "/sys/class/leds/fan/brightness"
    private const val FAN_LIGHT_TRIGGER_PATH = "/data/system/lingxi_fan_light.trigger"

    /**
     * 原厂灵犀键「常亮灯效」开关对应的设置键（Settings.Global）。
     * 实测：在设置-风扇与氛围灯-氛围灯-常亮灯效里切换开关时，只有这个键变化
     * （0=关 1=开），底层由 oplus-services 里的监听服务驱动 AW22xxx 氛围灯。
     */
    private const val AMBIENT_LIGHT_SETTING = "customize_breath_light_ambient"
        /** 灯效 qs tile：点击等效于快捷面板里的「氛围灯效」开关。 */
        private const val ATMOSPHERE_LIGHT_TILE =
            "com.android.settings/com.oplus.settings.feature.display.atmospherelight.AtmosphereLightEffectTileService"
        private const val GAME_ASSISTANT_PACKAGE = "com.oplus.games"
        private const val GAME_ASSISTANT_ACTIVITY =
            "com.oplus.games/business.module.desktop.JumpSpaceActivity"
        private const val GAME_ASSISTANT_ACTION = "com.oplus.games.action.desktop.space.main"
        private val RECORDING_PACKAGES = listOf(
            "com.coloros.soundrecorder",
            "com.oneplus.soundrecorder"
        )
    }

    private var context: Context? = null
    private var strategy: Any? = null
    private var torchEnabled = false
    private var flashlightMessenger: Messenger? = null
    private var flashlightServiceConnection: ServiceConnection? = null
    private var flashlightBindPending = false
    private val flashlightReplyMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            XposedBridge.log("LingXiKeyCustom: flashlight service reply what=${message.what}")
        }
    })
    private var dndMessenger: Messenger? = null
    private var dndServiceConnection: ServiceConnection? = null
    private var dndBindPending = false
    private val dndReplyMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            XposedBridge.log("LingXiKeyCustom: DND service reply what=${message.what}")
        }
    })
    private var ringMessenger: Messenger? = null
    private var ringServiceConnection: ServiceConnection? = null
    private var ringBindPending = false
    private val ringReplyMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            XposedBridge.log("LingXiKeyCustom: ring-mode seedling reply what=${message.what}")
        }
    })
    private var screenshotHelper: Any? = null
    private var screenshotMethod: java.lang.reflect.Method? = null

    fun updateContext(value: Context?) {
        if (value != null) context = value
    }

    fun updateStrategy(value: Any?) {
        if (value != null) strategy = value
    }

    fun vibrateInstantCue() {
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        vibrateInstant(currentContext)
    }

    /** 按下灵犀键时的即时触感：一次短促 tick（单击一下、双击两下）。 */
    fun vibratePressCue() = vibrateInstantCue()

    /**
     * 长按达到阈值后的持续触感。
     *
     * 用一次超长 one-shot 振动 + 松手 cancel()，比循环发短振动更接近「一直振动」的手感，
     * 也不会因为 Handler 抖动而断续。给一个兜底时长，避免松手事件丢失后震个不停。
     */
    fun startLongPressHaptic() {
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        runCatching {
            val vibrator = currentContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(LONG_PRESS_HAPTIC_MS, 128))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(LONG_PRESS_HAPTIC_MS)
            }
            XposedBridge.log("LingXiKeyCustom: long-press haptic started")
        }.onXposedFailure("start long-press haptic")
    }

    /** 松手 / 取消手势时停止持续触感。 */
    fun stopLongPressHaptic() {
        val currentContext = context ?: return
        runCatching {
            val vibrator = currentContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            vibrator.cancel()
        }.onXposedFailure("stop long-press haptic")
    }

    fun notifyUnknownMorseSequence(settings: AppSettings) {
        if (!settings.unknownMorseFeedbackEnabled) return
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        showToast(currentContext, settings.unknownMorseToastText.ifBlank { DEFAULT_UNKNOWN_MORSE_TOAST })
        vibrateInstant(currentContext)
        Handler(Looper.getMainLooper()).postDelayed({ vibrateInstantCue() }, 100L)
    }

    fun executeCombination(action: SideKeyCombinationAction) {
        if (action == SideKeyCombinationAction.NONE) return
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        runCatching {
            val keyCode = when (action) {
                SideKeyCombinationAction.MEDIA_PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                SideKeyCombinationAction.MEDIA_NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
                SideKeyCombinationAction.MEDIA_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                SideKeyCombinationAction.NONE -> return@runCatching
            }
            val downTime = android.os.SystemClock.uptimeMillis()
            val audio = currentContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.dispatchMediaKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(downTime, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
            XposedBridge.log("LingXiKeyCustom: executed side-key combination action=$action")
        }.onXposedFailure("execute side-key combination")
    }

    fun execute(
        action: ActionType,
        custom: CustomActionSettings,
        settings: AppSettings,
        interactive: Boolean = true,
        wakeScreen: Boolean = false,
        vibrationEnabled: Boolean = settings.vibrationEnabled
    ) {
        XposedBridge.log("LingXiKeyCustom: executing action=$action")
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        runCatching {
            if (wakeScreen || (!interactive && settings.wakeScreenWhenOff)) {
                // Oplus' strategy exposes the same wakeup operation used by its stock shortcut.
                runCatching { strategy?.let { XposedHelpers.callMethod(it, "wakeup") } }
                    .onXposedFailure("wake screen")
            }
            var cycleMode: Int? = null
            when (action) {
                ActionType.SHOW_RINGER -> showRingerMode(currentContext)
                ActionType.CYCLE_RINGER -> cycleMode = cycleRinger(currentContext)
                ActionType.TOGGLE_DND -> toggleDnd(currentContext)
                ActionType.FLASHLIGHT -> toggleTorch(currentContext)
                ActionType.COOLING_FAN_TOGGLE -> toggleCoolingFan()
                ActionType.FAN_LIGHT_TOGGLE -> toggleFanLight()
                ActionType.RECORDING -> toggleRecording(currentContext)
                ActionType.SCREENSHOT -> requestScreenshot(currentContext)
                ActionType.COMMON_FUNCTION -> executeCommon(currentContext, custom.commonAction)
                ActionType.XIAOBU_SHORTCUT -> executeXiaobuShortcut(currentContext, custom.xiaobuShortcutId)
                ActionType.XIAOBU_SCREEN_RECOGNIZE -> executeXiaobuScreenRecognize(currentContext)
                ActionType.GAME_ASSISTANT -> openGameAssistant(currentContext)
                ActionType.LAUNCH_APP -> launchApp(currentContext, custom.launchAppPackage)
                ActionType.CUSTOM_ACTIVITY -> startCustomActivity(currentContext, custom)
                ActionType.CUSTOM_URL -> openUrl(currentContext, custom.urlScheme)
                ActionType.SHELL_COMMAND -> executeShell(custom.shellCommand, custom.shellToastEnabled)
                ActionType.CAMERA_SHUTTER -> broadcastToCamera(CameraShutterBridge.ACTION_PRESS)
                ActionType.MUSIC_PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                ActionType.MUSIC_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                ActionType.MUSIC_PREVIOUS -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                ActionType.VIDEO_SEEK_FORWARD -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                ActionType.VIDEO_SEEK_BACKWARD -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND)
                ActionType.NONE -> return
            }
            feedback(currentContext, action, settings, cycleMode, vibrationEnabled)
        }.onXposedFailure("execute action")
    }

    private fun resolveSystemContext(): Context? = ContextResolver.systemContext()

    private fun showRingerMode(context: Context) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sendRingModeSeedling(context, getRingerModeInternal(audio))
    }

    private fun cycleRinger(context: Context): Int {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nextMode = when (getRingerModeInternal(audio)) {
            RINGER_MODE_NORMAL -> RINGER_MODE_VIBRATE
            RINGER_MODE_VIBRATE -> RINGER_MODE_SILENT
            else -> RINGER_MODE_NORMAL
        }
        // Match ActionKeyStartApp.w(): send the Seedling event and then update the same
        // internal AudioManager state used by the stock action.
        sendRingModeSeedling(context, nextMode)
        setRingerMode(audio, nextMode)
        return nextMode
    }

    /** ActionKeyStartApp reads the internal state before cycling; public ringerMode is fallback. */
    private fun getRingerModeInternal(audio: AudioManager): Int = runCatching {
        val method = audio.javaClass.methods.firstOrNull {
            it.name == "getRingerModeInternal" && it.parameterTypes.isEmpty()
        } ?: return@runCatching audio.ringerMode
        method.isAccessible = true
        (method.invoke(audio) as? Int) ?: audio.ringerMode
    }.onXposedFailure("read internal ringer mode").getOrDefault(audio.ringerMode)

    private fun setRingerMode(audio: AudioManager, mode: Int) {
        val internal = audio.javaClass.methods.firstOrNull { method ->
            method.name == "setRingerModeInternal" && method.parameterTypes.isNotEmpty()
        }
        if (internal != null) {
            runCatching {
                val args = internal.parameterTypes.mapIndexed { index, type ->
                    when {
                        type == Int::class.javaPrimitiveType -> mode
                        type == String::class.java -> if (index == 1) "customsidebuttonfunctions" else "side_key"
                        type == Boolean::class.javaPrimitiveType -> false
                        else -> null
                    }
                }.toTypedArray()
                internal.isAccessible = true
                internal.invoke(audio, *args)
            }.onXposedFailure("set internal ringer mode").onSuccess { return }
        }
        audio.ringerMode = mode
    }

    private fun toggleDnd(context: Context) {
        // ColorOS SystemUI owns ZenModeController and the associated fluid-cloud/UI effects.
        if (toggleDndThroughSystemUi(context)) return

        // Keep this framework path for ROMs without the Oplus service.
        val notification = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notification.isNotificationPolicyAccessGranted) {
            showToast(context, "请先授予免打扰访问权限")
            return
        }
        notification.setInterruptionFilter(
            if (notification.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            else android.app.NotificationManager.INTERRUPTION_FILTER_NONE
        )
    }

    private fun toggleDndThroughSystemUi(context: Context): Boolean {
        dndMessenger?.let { messenger -> return sendDndCommand(messenger) }
        if (dndBindPending) return true
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                dndBindPending = false
                val messenger = Messenger(service)
                dndMessenger = messenger
                XposedBridge.log("LingXiKeyCustom: connected to $name")
                if (!sendDndCommand(messenger)) dndMessenger = null
            }

            override fun onServiceDisconnected(name: ComponentName) {
                dndMessenger = null
                dndBindPending = false
                XposedBridge.log("LingXiKeyCustom: disconnected from $name")
            }
        }
        val intent = Intent().setComponent(ComponentName(SYSTEM_UI_PACKAGE, DND_SERVICE))
        dndServiceConnection = connection
        dndBindPending = true
        val bound = runCatching {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onXposedFailure("bind DND service").getOrDefault(false)
        if (!bound) {
            dndServiceConnection = null
            dndBindPending = false
            return false
        }
        XposedBridge.log("LingXiKeyCustom: binding $DND_SERVICE")
        return true
    }

    private fun sendDndCommand(messenger: Messenger): Boolean = runCatching {
        val message = Message.obtain().apply {
            what = 2 // KeyBaseService: handleActionButtonLongPress()
            data = Bundle().apply { putString("key", "LONG_PRESS") }
            replyTo = dndReplyMessenger
        }
        messenger.send(message)
        XposedBridge.log("LingXiKeyCustom: sent DND long-press command")
        true
    }.onXposedFailure("send DND service command").getOrDefault(false)

    private fun sendRingModeSeedling(context: Context, mode: Int): Boolean {
        ringMessenger?.let { messenger -> return sendRingModeMessage(messenger, mode) }
        if (ringBindPending) return true
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                ringBindPending = false
                val messenger = Messenger(service)
                ringMessenger = messenger
                XposedBridge.log("LingXiKeyCustom: connected to $name")
                if (!sendRingModeMessage(messenger, mode)) ringMessenger = null
            }

            override fun onServiceDisconnected(name: ComponentName) {
                ringMessenger = null
                ringBindPending = false
                XposedBridge.log("LingXiKeyCustom: disconnected from $name")
            }
        }
        val intent = Intent(SEEDLING_ACTION).setPackage(SYSTEM_UI_PACKAGE)
        ringServiceConnection = connection
        ringBindPending = true
        val bound = runCatching {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onXposedFailure("bind ring-mode seedling service").getOrDefault(false)
        if (!bound) {
            ringServiceConnection = null
            ringBindPending = false
            return false
        }
        XposedBridge.log("LingXiKeyCustom: binding ring-mode seedling service")
        return true
    }

    private fun sendRingModeMessage(messenger: Messenger, mode: Int): Boolean = runCatching {
        val message = Message.obtain().apply {
            what = 1 // BaseService: seedling update
            data = Bundle().apply {
                putInt("ringModeType", mode)
                putString("seedling_event", "ringModeEvent")
            }
            replyTo = ringReplyMessenger
        }
        messenger.send(message)
        XposedBridge.log("LingXiKeyCustom: sent ring-mode seedling mode=$mode")
        true
    }.onXposedFailure("send ring-mode seedling command").getOrDefault(false)

    /** Uses the same ColorOS action-button entry that starts or finishes a recording. */
    private fun toggleRecording(context: Context) {
        val packageName = RECORDING_PACKAGES.firstOrNull { packageName ->
            val intent = Intent(RECORDING_ACTION).setPackage(packageName)
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }
        if (packageName == null) {
            XposedBridge.log("LingXiKeyCustom: ColorOS recorder activity is unavailable")
            return
        }
        val intent = Intent(RECORDING_ACTION)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val startedAsCurrentUser = runCatching {
            XposedHelpers.callMethod(
                context,
                "startActivityAsUser",
                intent,
                UserHandle.getUserHandleForUid(Process.myUid())
            )
            true
        }.onXposedFailure("start system recorder as current user").getOrDefault(false)
        if (!startedAsCurrentUser) context.startActivity(intent)
        XposedBridge.log("LingXiKeyCustom: triggered system recorder action package=$packageName")
    }

    private fun toggleTorch(context: Context) {
        // ColorOS owns the flashlight state in SystemUI. Its stock action binds this service and
        // sends the long-press command, which toggles the flashlight through FlashlightController
        // and updates ColorOS' animation/state integration.
        val success = toggleTorchThroughSystemUi(context) || toggleTorchThroughDefault(context)

        // update status
        torchEnabled = !torchEnabled
    }

    private fun toggleTorchThroughSystemUi(context: Context): Boolean {
        if (torchEnabled) {
            // 当已经启用的时候, 就是通过系统 UI 切换了
            return false
        }

        XposedBridge.log("try system ui toggle")

        flashlightMessenger?.let { messenger ->
            return sendFlashlightCommand(messenger)
        }
        if (flashlightBindPending) return true

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                flashlightBindPending = false
                val messenger = Messenger(service)
                flashlightMessenger = messenger
                XposedBridge.log("LingXiKeyCustom: connected to $name")
                if (!sendFlashlightCommand(messenger)) {
                    flashlightMessenger = null
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                flashlightMessenger = null
                flashlightBindPending = false
                XposedBridge.log("LingXiKeyCustom: disconnected from $name")
            }
        }
        val intent = Intent().setComponent(ComponentName(SYSTEM_UI_PACKAGE, FLASHLIGHT_SERVICE))
        flashlightServiceConnection = connection
        flashlightBindPending = true
        val bound = runCatching {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onFailure {
            XposedBridge.log("LingXiKeyCustom: cannot bind flashlight service: ${it.message}")
        }.getOrDefault(false)
        if (!bound) {
            flashlightServiceConnection = null
            flashlightBindPending = false
            return false
        }
        XposedBridge.log("LingXiKeyCustom: binding $FLASHLIGHT_SERVICE")
        return true
    }

    private fun toggleTorchThroughDefault(context: Context): Boolean {

        XposedBridge.log("try camera control toggle")
        // Keep the CameraManager path for ROMs without an exposed vendor strategy (and for the
        // generic PhoneWindowManager/raw-input fallbacks).
        val camera = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = camera.cameraIdList.firstOrNull { id ->
            camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        val torchNewState = !torchEnabled
        camera.setTorchMode(cameraId, torchNewState)
        return true
    }

    private fun sendFlashlightCommand(messenger: Messenger): Boolean = runCatching {
        val message = Message.obtain().apply {
            what = 2 // KeyBaseService: handleActionButtonLongPress()
            data = Bundle().apply { putString("key", "LONG_PRESS") }
            replyTo = flashlightReplyMessenger
        }
        messenger.send(message)
        XposedBridge.log("LingXiKeyCustom: sent flashlight long-press command")
        true
    }.onFailure {
        XposedBridge.log("LingXiKeyCustom: flashlight service command failed: ${it.message}")
    }.getOrDefault(false)

    private fun requestScreenshot(context: Context) {
        if (requestScreenshotWithHelper(context)) return
        val statusBar = context.getSystemService("statusbar")
        val requested = runCatching {
            val method = statusBar?.javaClass?.declaredMethods?.firstOrNull {
                it.name == "requestScreenshot"
            } ?: statusBar?.javaClass?.methods?.firstOrNull { it.name == "requestScreenshot" }
                ?: return@runCatching false
            method.isAccessible = true
            val arguments = method.parameterTypes.map { type ->
                when {
                    type == Int::class.javaPrimitiveType -> 0
                    type == Long::class.javaPrimitiveType -> 0L
                    type == Boolean::class.javaPrimitiveType -> false
                    type == String::class.java -> "side_key"
                    else -> null
                }
            }.toTypedArray()
            method.invoke(statusBar, *arguments)
            true
        }.onXposedFailure("request screenshot through status bar").getOrDefault(false)
        if (!requested) {
            executeShell("service call color_screenshot 1") {
                context.sendBroadcast(
                    Intent("com.android.systemui.action.SCREENSHOT").setPackage("com.android.systemui")
                )
                context.sendBroadcast(Intent("android.intent.action.SCREENSHOT").setPackage("com.android.systemui"))
            }
        }
    }

    /** Uses the framework's ScreenshotHelper so the request reaches SystemUI without a shell. */
    private fun requestScreenshotWithHelper(context: Context): Boolean = runCatching {
        val helper = screenshotHelper ?: Class.forName("com.android.internal.util.ScreenshotHelper")
            .getConstructor(Context::class.java)
            .newInstance(context)
            .also { screenshotHelper = it }
        val method = screenshotMethod ?: helper.javaClass.methods.firstOrNull { candidate ->
            if (candidate.name != "takeScreenshot") return@firstOrNull false
            val types = candidate.parameterTypes
            (types.size == 3 && types[0] == Int::class.javaPrimitiveType &&
                Handler::class.java.isAssignableFrom(types[1])) ||
                (types.size == 6 && types[0] == Int::class.javaPrimitiveType &&
                    types[1] == Boolean::class.javaPrimitiveType &&
                    types[2] == Boolean::class.javaPrimitiveType &&
                    types[3] == Int::class.javaPrimitiveType &&
                    Handler::class.java.isAssignableFrom(types[4]))
        } ?: error("ScreenshotHelper.takeScreenshot is unavailable")
            .also { screenshotMethod = it }

        val callback = Consumer<Any?> { result ->
            if (result == null) {
                XposedBridge.log("LingXiKeyCustom: ScreenshotHelper returned no URI")
            }
        }
        val handler = Handler(Looper.getMainLooper())
        method.isAccessible = true
        when (method.parameterTypes.size) {
            3 -> method.invoke(helper, 0, handler, callback)
            6 -> method.invoke(helper, 1, true, true, 0, handler, callback)
            else -> error("Unsupported ScreenshotHelper signature")
        }
        XposedBridge.log("LingXiKeyCustom: screenshot requested through ScreenshotHelper")
        true
    }.onXposedFailure("request screenshot through ScreenshotHelper").getOrDefault(false)

    private fun executeCommon(context: Context, action: CommonAction) {
        when (action) {
            CommonAction.WECHAT_PAY -> startWechatShortcut(context, "launch_type_offline_wallet")
            CommonAction.WECHAT_SCAN -> startWechatShortcut(context, "launch_type_scan_qrcode")
            CommonAction.ALIPAY_PAY -> openUrl(context, "alipays://platformapi/startapp?saId=20000056")
            CommonAction.ALIPAY_RECEIVE -> openUrl(context, "alipays://platformapi/startapp?appId=20000123")
            CommonAction.ALIPAY_SCAN -> openUrl(context, "alipays://platformapi/startapp?saId=10000007")
            CommonAction.FLASH_MEMORY -> startFlashMemory()
            CommonAction.XIAOBU_MEMORY -> startActivity(context, "com.oplus.aimemory", "com.oplus.aimemory.MainActivity", "")
        }
    }

    private fun startWechatShortcut(context: Context, launchType: String) {
        val intent = Intent("com.tencent.mm.ui.ShortCutDispatchAction")
            .setComponent(ComponentName("com.tencent.mm", "com.tencent.mm.ui.ShortCutDispatchActivity"))
            .setPackage("com.tencent.mm")
            .putExtra("LauncherUI.Shortcut.LaunchType", launchType)
            .putExtra("LauncherUI.From.Scaner.Shortcut", false)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

    private fun startActivity(
        context: Context,
        packageName: String,
        className: String,
        action: String,
        launchInSmallWindow: Boolean = false
    ) {
        val intent = if (className.isBlank()) {
            context.packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setPackage(packageName)
        } else {
            Intent().setComponent(ComponentName(packageName, className))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (action.isNotBlank()) intent.action = action

        if (launchInSmallWindow) {
            val launched = runCatching {
                val options = ActivityOptions.makeBasic()
                XposedHelpers.callMethod(options, "setLaunchWindowingMode", SMALL_WINDOW_MODE)
                context.startActivity(intent, options.toBundle())
                true
            }.onXposedFailure("launch Activity in small-window mode").getOrDefault(false)
            if (launched) return
            XposedBridge.log("LingXiKeyCustom: small-window launch unavailable, falling back to normal launch")
        }
        context.startActivity(intent)
    }

    private fun startCustomActivity(context: Context, custom: CustomActionSettings) {
        if (custom.activityPackage.isBlank()) {
            XposedBridge.log("LingXiKeyCustom: custom Activity package is empty")
            return
        }
        startActivity(
            context,
            custom.activityPackage,
            custom.activityClass,
            custom.activityAction,
            custom.launchInSmallWindow
        )
    }

    /**
     * 「打开应用」：优先用 PackageManager 解析启动入口，解析不到再退回
     * ACTION_MAIN + CATEGORY_LAUNCHER 的隐式 Intent（部分 ROM 的 getLaunchIntentForPackage
     * 对 SystemUI/设置类应用返回 null，但 launcher Intent 仍可拉起）。
     */
    private fun launchApp(context: Context, packageName: String) {
        if (packageName.isBlank()) {
            XposedBridge.log("LingXiKeyCustom: launch app package is empty")
            return
        }
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        val launchIntent = runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }
            .onXposedFailure("resolve launch intent for $packageName")
            .getOrNull()
        if (launchIntent != null) {
            launchIntent.addFlags(flags)
            context.startActivity(launchIntent)
            XposedBridge.log("LingXiKeyCustom: launched $packageName via launch intent")
            return
        }
        runCatching {
            val fallback = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName)
                .addFlags(flags)
            context.startActivity(fallback)
            XposedBridge.log("LingXiKeyCustom: launched $packageName via launcher intent")
        }.onXposedFailure("launch app $packageName").onFailure {
            showToast(context, "无法打开该应用")
        }
    }

    /**
     * 「打开游戏助手」。
     *
     * OPPO 游戏助手（com.oplus.games）没有 LAUNCHER 入口，所以不会出现在应用列表里，
     * 原厂是直接用组件 Intent 拉起的。实测 JumpSpaceActivity + desktop.space.main
     * 会进入游戏空间主界面；该 Activity 带 oppo.permission.OPPO_COMPONENT_SAFE 保护，
     * 我们的代码运行在 system_server（持有平台签名权限），因此可以直接启动。
     */
    private fun openGameAssistant(context: Context) {
        val component = ComponentName.unflattenFromString(GAME_ASSISTANT_ACTIVITY)
        runCatching {
            if (component != null) {
                context.startActivity(
                    Intent(GAME_ASSISTANT_ACTION)
                        .setComponent(component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                XposedBridge.log("LingXiKeyCustom: opened game assistant via $component")
                return
            }
            error("no component")
        }.onXposedFailure("open game assistant").onFailure {
            // 兜底：部分 ROM 用隐式 action 注册
            runCatching {
                context.startActivity(
                    Intent("oplus.intent.action.GAME_ASSISTANT").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onXposedFailure("open game assistant fallback").onFailure {
                showToast(context, "未找到游戏助手")
            }
        }
    }

    private fun openUrl(context: Context, value: String) {
        if (value.isBlank()) {
            XposedBridge.log("LingXiKeyCustom: custom Url Scheme is empty")
            return
        }
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(value))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun startFlashMemory() {
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        runCatching {
            val intent = resolveFlashNotesIntent(currentContext)
                ?: error("no compatible Flash Notes service found")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentContext.startForegroundService(intent)
            } else {
                currentContext.startService(intent)
            }
            XposedBridge.log("LingXiKeyCustom: triggered ColorOS Flash Notes service")
        }.onXposedFailure("start Flash Notes service")
    }

    /**
     * Flash Notes moved packages/classes across ColorOS releases. Resolve the installed service
     * instead of using a ColorOS version cutoff, since regional builds do not share exact versions.
     */
    private fun resolveFlashNotesIntent(context: Context): Intent? {
        val candidates = listOf(
            Candidate(
                packageName = "com.oplus.gleanerservice",
                className = "com.oplus.gleanerservice.flashnotes.business.service.DataCollectService",
                action = "oplus.gleanerservice.intent.action.COLLECT_DATA"
            ),
            Candidate(
                packageName = "com.coloros.colordirectservice",
                className = "com.oplus.directservice.flashnotes.business.service.DataCollectService",
                action = "coloros.colordirectservice.intent.action.COLLECT_DATA"
            ),
        )
        val packageManager = context.packageManager
        for (candidate in candidates) {
            val component = ComponentName(candidate.packageName, candidate.className)
            val serviceInfo = runCatching {
                packageManager.getServiceInfo(component, PackageManager.MATCH_ALL)
            }.onXposedFailure("resolve Flash Notes service $component").getOrNull() ?: continue
            if (!serviceInfo.enabled) continue
            val intent = Intent(candidate.action)
                .setComponent(component)
                .putExtra("triggerType", 1)
                .putExtra("longPressEventType", 0)
            if (packageManager.resolveService(intent, PackageManager.MATCH_ALL) != null) {
                XposedBridge.log("LingXiKeyCustom: resolved Flash Notes service $component")
                return intent
            }
        }
        return null
    }

    private data class Candidate(
        val packageName: String,
        val className: String,
        val action: String
    )

    private fun executeXiaobuShortcut(context: Context, shortcutId: String) {
        if (shortcutId.isBlank()) {
            XposedBridge.log("LingXiKeyCustom: Xiaobu shortcut id is empty")
            return
        }
        val params = Bundle().apply {
            putString("tag", shortcutId)
            putString("widgetCode", "")
        }
        context.contentResolver.call(
            Uri.parse("content://com.coloros.shortcuts.basecard.provider.FunctionSpecProvider"),
            "execute_one_shortcut",
            null,
            params
        )
    }

    /** Sends a control broadcast to the camera process (its module bridge presses the shutter). */
    private fun broadcastToCamera(action: String) {
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        runCatching {
            val intent = Intent(action)
                .setPackage("com.oplus.camera")
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .addFlags(FLAG_RECEIVER_INCLUDE_BACKGROUND)
            currentContext.sendBroadcast(intent)
            XposedBridge.log("LingXiKeyCustom: sent camera broadcast $action")
        }.onXposedFailure("send camera broadcast")
    }

    /** Dispatches a plain key through the window manager so the foreground app receives it. */
    private fun injectKey(keyCode: Int) {
        runCatching {
            val downTime = android.os.SystemClock.uptimeMillis()
            val inputManager = Class.forName("android.hardware.input.InputManager")
                .getMethod("getInstance").invoke(null)
            val inject = inputManager.javaClass.getMethod(
                "injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType
            )
            val down = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(downTime, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
            inject.invoke(inputManager, down, 0)
            inject.invoke(inputManager, up, 0)
            XposedBridge.log("LingXiKeyCustom: injected keyCode=$keyCode")
        }.onXposedFailure("inject key $keyCode")
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val currentContext = context ?: resolveSystemContext()?.also { context = it } ?: return
        runCatching {
            val downTime = android.os.SystemClock.uptimeMillis()
            val audio = currentContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.dispatchMediaKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(downTime, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
        }.onXposedFailure("dispatch media key $keyCode")
    }

    /**
     * 小布识屏。
     *
     * ColorOS 把「长按屏幕底部手势条」映射到系统的 assist 长按路径
     * （AssistManager.startAssist，带 longPress 语义），识屏浮层由系统框架 / SystemUI
     * 接管。本模块运行在 system_server 进程内，因此优先直接反射调用 AssistManager；
     * 失败时依次退化为 ACTION_ASSIST 带长按标记、以及注入 KEYCODE_ASSIST 长按。
     *
     * 返回 true 表示至少有一条路径成功发起。
     */
    private fun executeXiaobuScreenRecognize(context: Context): Boolean {
        // 路径 1：ColorOS 识屏正式入口（真机验证成功）。
        // SystemUI 长按手势条 → OcrScreenService(StartUpType=0)
        //   → com.coloros.colordirectservice/.InvokeEntranceService
        //   → CollectService → CollectInfoActivity + FloatingScan 识屏浮窗。
        // 该服务非 exported 且需 oppo.permission.OPPO_COMPONENT_SAFE，
        // shell 无法调用；模块运行在 system_server(uid 1000)，可直接 startForegroundService。
        if (startXiaobuRecognizeService()) return true
        // 路径 2：直接调用 system_server 里的 AssistManager（最贴近原生长按手势条行为）。
        if (invokeAssistManager()) return true
        // 路径 3：ACTION_ASSIST + 长按标记（部分 ColorOS 版本据此切到识屏）。
        if (startAssistIntent()) return true
        // 路径 4：注入 KEYCODE_ASSIST 长按（走 InputManager 正常分发链）。
        if (injectAssistLongPress()) return true
        XposedBridge.log("LingXiKeyCustom: Xiaobu screen recognize failed on all paths")
        return false
    }

    /**
     * 拉起 ColorOS 真正的「小布识屏」（半屏浮层：提取文字 / 翻译 / 转文档）。
     *
     * 实测链路（ColorOS 16, PMG110）：
     * `startForegroundService(Intent("com.coloros.directui.InvokeEntranceService")
     *    .setClassName("com.coloros.colordirectservice", "com.coloros.colordirectservice.InvokeEntranceService")
     *    .putExtra("StartUpType", 0))`
     * 成功后浮窗名为 collection_activity_start。
     * 注意：必须用 startForegroundService，普通 startService 会命中后台启动限制；
     * 需要额外 StartUpType=0（与 SystemUI OcrScreenService 传参一致）。
     */
    private fun startXiaobuRecognizeService(): Boolean = runCatching {
        val context = context ?: resolveSystemContext() ?: return false
        val intent = Intent("com.coloros.directui.InvokeEntranceService")
            .setClassName(
                "com.coloros.colordirectservice",
                "com.coloros.colordirectservice.InvokeEntranceService"
            )
            .putExtra("StartUpType", 0)
        context.startForegroundService(intent)
        XposedBridge.log("LingXiKeyCustom: started InvokeEntranceService (Xiaobu screen recognize)")
        true
    }.onXposedFailure("start InvokeEntranceService").getOrDefault(false)

    /** 反射调用 android.app.assist.AssistManager#startAssist，长按语义等同于长按手势条。 */
    private fun invokeAssistManager(): Boolean {
        val threadClass = runCatching { Class.forName("android.app.ActivityThread") }
            .onXposedFailure("load ActivityThread").getOrNull() ?: return false
        // systemThread() 在部分版本不存在或抛异常，逐个安全尝试。
        val systemThread = runCatching {
            threadClass.getMethod("systemThread").invoke(null)
        }.onXposedFailure("ActivityThread.systemThread").getOrNull()
        val currentThread = runCatching {
            threadClass.getMethod("currentActivityThread").invoke(null)
        }.onXposedFailure("ActivityThread.currentActivityThread").getOrNull()
        val target = systemThread ?: currentThread ?: return false
        return runCatching {
            // mAssistManager 是 ActivityThread 的私有字段。
            val field = threadClass.getDeclaredField("mAssistManager").apply { isAccessible = true }
            val manager = field.get(target) ?: return false
            val managerClass = manager.javaClass

            val bundle = android.os.Bundle()
            bundle.putBoolean("key_assist_long_press", true)
            bundle.putBoolean("invoked_from_gesture_navigation", true)

            // 不同版本签名不同，逐个尝试。
            val candidates = managerClass.declaredMethods.filter { it.name == "startAssist" }
            for (method in candidates) {
                method.isAccessible = true
                val ok = runCatching {
                    when (method.parameterTypes.size) {
                        0 -> { method.invoke(manager); true }
                        1 -> {
                            if (method.parameterTypes[0] == android.os.Bundle::class.java) {
                                method.invoke(manager, bundle); true
                            } else false
                        }
                        2 -> {
                            if (method.parameterTypes[0] == android.os.Bundle::class.java) {
                                method.invoke(manager, bundle, false); true
                            } else false
                        }
                        else -> false
                    }
                }.getOrDefault(false)
                if (ok) {
                    XposedBridge.log("LingXiKeyCustom: AssistManager.startAssist invoked via ${method.parameterTypes.size} args")
                    return true
                }
            }
            false
        }.onXposedFailure("invoke AssistManager").getOrDefault(false)
    }

    /** ACTION_ASSIST + 长按标记，交给系统解析。 */
    private fun startAssistIntent(): Boolean = runCatching {
        val context = context ?: resolveSystemContext() ?: return false
        val intent = Intent(Intent.ACTION_ASSIST)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .putExtra("key_assist_long_press", true)
            .putExtra("invoked_from_gesture_navigation", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.onXposedFailure("start assist long-press intent").getOrDefault(false)

    /** 注入 KEYCODE_ASSIST 的长按（DOWN 保持超过长按阈值后再 UP）。 */
    private fun injectAssistLongPress(): Boolean = runCatching {
        val inputManager = Class.forName("android.hardware.input.InputManager")
            .getMethod("getInstance").invoke(null)
            ?: return false
        val injectMethod = inputManager.javaClass.methods
            .firstOrNull { it.name == "injectInputEvent" && it.parameterTypes.size == 2 }
            ?: return false
        val downTime = android.os.SystemClock.uptimeMillis()
        val down = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ASSIST, 0)
        val up = KeyEvent(downTime, downTime + ASSIST_LONG_PRESS_MS, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ASSIST, 0)
        injectMethod.invoke(inputManager, down, 0)
        Thread.sleep(ASSIST_LONG_PRESS_MS)
        injectMethod.invoke(inputManager, up, 0)
        XposedBridge.log("LingXiKeyCustom: injected KEYCODE_ASSIST long press")
        true
    }.onXposedFailure("inject assist long press").getOrDefault(false)

    private fun executeShell(command: String, showToast: Boolean = true, onFailure: (() -> Unit)? = null) {
        if (command.isBlank()) {
            XposedBridge.log("LingXiKeyCustom: shell command is empty")
            return
        }
        Thread {
            val currentContext = context ?: return@Thread
            if (!ShellCommandRunner.executeAsRoot(currentContext, command, showToast)) onFailure?.invoke()
        }.apply {
            isDaemon = true
            name = "CustomSideButtonShell"
            start()
        }
    }

    /**
     * 切换机身内置散热风扇（原厂方式）。
     *
     * 逆向结论（2026-10-13 实测确认）：
     * - 写单个 cooling_fan_main_switch 不驱动硬件（它只是 UI 状态键）；
     * - 真正的原厂控制链：com.oplus.exsystemservice 里的 OplusFanService 通过
     *   ContentObserver 监听 cooling_fan_main_switch_modify（控制信号键），
     *   收到变更后读 main_switch 判定目标状态，经 Misc HAL 写 /sys/class/leds/fan/state，
     *   并联动"智能场景判断 + 风速决策 + 设置页/快捷面板 UI 同步"；
     * - 流体云胶囊（「已开启/已关闭」）也由 OplusFanService 触发：它 bind
     *   com.oplus.devicesettings/.FanNotificationService，type=fluid_cloud_fan_on/off。
     * - 原厂设置页点击开关 = 同时写 main_switch + main_switch_modify（同值）。
     *
     * 因此本模块直接写这两个键（system uid 对 Settings.System 有写权限），
     * 完整复用原厂服务：硬件控制、胶囊通知、UI 同步全部自动完成，
     * 原先的 root 守护进程方案（lingxi_fan_daemon.sh + lingxi_fan.trigger）不再需要。
     */
    private fun toggleCoolingFan() {
        val cr = context?.contentResolver ?: return
        val result = runCatching {
            val current = android.provider.Settings.System.getInt(cr, FAN_MAIN_SWITCH, 0)
            val next = if (current == 1) 0 else 1
            android.provider.Settings.System.putInt(cr, FAN_MAIN_SWITCH, next)
            android.provider.Settings.System.putInt(cr, FAN_MAIN_SWITCH_MODIFY, next)
            XposedBridge.log("LingXiKeyCustom: cooling fan toggle via settings: $current -> $next")
            true
        }.onXposedFailure("cooling fan toggle via settings").getOrDefault(false)
        if (!result) {
            // 兜底：写键失败时退回应用侧 root shell 直写 sysfs
            val script = buildString {
                append("v=$(cat /sys/class/leds/fan/state 2>/dev/null); ")
                append("case \"${'$'}v\" in 1) n=0;; *) n=1;; esac; ")
                append("echo ${'$'}n > /sys/class/leds/fan/state; ")
                append("s=$(settings get system cooling_fan_main_switch); ")
                append("case \"${'$'}s\" in 1) m=0;; *) m=1;; esac; ")
                append("settings put system cooling_fan_main_switch ${'$'}m; ")
                append("settings put system cooling_fan_main_switch_modify ${'$'}m")
            }
            val currentContext = context ?: return
            ShellCommandRunner.executeAsRoot(currentContext, script, showToast = false)
        }
    }

    /**
     * 切换机身灯效（对应原厂灵犀键的「常亮灯效」功能）。
     *
     * 逆向结论：原厂在 设置-风扇与氛围灯-氛围灯-常亮灯效 的开关就是写
     * Settings.Global customize_breath_light_ambient（0/1），oplus-services 中有监听服务
     * 据此驱动 AW22xxx 氛围灯。本模块恰好也运行在 system_server，用系统 context 写该键
     * 即等价于原厂行为，氛围灯服务会收到变更回调。
     *
     * 路径 1（主）：写 Settings.Global customize_breath_light_ambient。
     * 路径 2（兜底）：写 /sys/class/leds/fan/brightness（风扇灯，需 root 守护进程）。
     */
    private fun toggleFanLight() {
        val currentContext = context ?: return
        Thread {
            // 路径 1：与原厂一致，切换 Global customize_breath_light_ambient
            val ambient = runCatching {
                val current = android.provider.Settings.Global.getInt(
                    currentContext.contentResolver, AMBIENT_LIGHT_SETTING, 0
                )
                val next = if (current == 1) 0 else 1
                android.provider.Settings.Global.putInt(
                    currentContext.contentResolver, AMBIENT_LIGHT_SETTING, next
                )
                val verify = android.provider.Settings.Global.getInt(
                    currentContext.contentResolver, AMBIENT_LIGHT_SETTING, -1
                )
                XposedBridge.log("LingXiKeyCustom: ambient light $current -> $next (verify=$verify)")
                verify == next
            }.onXposedFailure("ambient light toggle").getOrDefault(false)
            if (ambient) return@Thread

            // 路径 2：兜底 —— 切换风扇灯亮度（0/255）
            val direct = runCatching {
                val current = readFanNode(FAN_LIGHT_PATH) ?: return@runCatching false
                val next = if (current > 0) "0" else "255"
                nativeWriteFile(FAN_LIGHT_PATH, next)
                val verify = readFanNode(FAN_LIGHT_PATH)
                XposedBridge.log("LingXiKeyCustom: fan light direct write $current -> $next (verify=$verify)")
                verify == next.toIntOrNull()
            }.onXposedFailure("fan light direct write").getOrDefault(false)
            if (direct) return@Thread

            if (signalFanLightDaemon()) return@Thread

            val script = buildString {
                append("v=$(cat ").append(FAN_LIGHT_PATH).append(" 2>/dev/null); ")
                append("case \"${'$'}v\" in 0) n=255;; *) n=0;; esac; ")
                append("echo ${'$'}n > ").append(FAN_LIGHT_PATH).append("; ")
                append("echo \"fan light -> ${'$'}(cat ").append(FAN_LIGHT_PATH).append(")\"")
            }
            XposedBridge.log("LingXiKeyCustom: fan light daemon unavailable, falling back to app trampoline")
            ShellCommandRunner.executeAsRoot(currentContext, script, showToast = false)
        }.apply {
            isDaemon = true
            name = "CustomSideButtonFanLight"
            start()
        }
    }

    /** 通过信令文件通知 root 守护进程切换灯效（与风扇互不干扰，用独立文件）。 */
    private fun signalFanLightDaemon(): Boolean = runCatching {
        val file = java.io.File(FAN_LIGHT_TRIGGER_PATH)
        val next = (System.currentTimeMillis() % 1_000_000L).toString()
        java.io.FileOutputStream(file, false).use { out ->
            out.write("$next\n".toByteArray())
            out.flush()
        }
        XposedBridge.log("LingXiKeyCustom: signaled fan-light daemon via $FAN_LIGHT_TRIGGER_PATH ($next)")
        true
    }.onXposedFailure("fan light daemon signal").getOrDefault(false)

    /**
     * 读 sysfs 节点（如风扇 state）。
     *
     * 早期版本走 libcore.io.Libcore 反射 open/read，但 Android 16 上 BlockGuardOs.open
     * 的反射签名不匹配（libcore.io.BlockGuardOs.open [String, int] 找不到），一律抛异常。
     * 改用标准 java.io：sysfs 的 read() 即是属性读取，行为与 `cat` 一致。
     */
    private fun readFanNode(path: String = FAN_LIGHT_PATH): Int? = runCatching {
        java.io.FileInputStream(path).use { input ->
            val buffer = ByteArray(32)
            val count = input.read(buffer)
            if (count <= 0) null else String(buffer, 0, count).trim().toIntOrNull()
        }
    }.onXposedFailure("read fan node").getOrNull()

    /**
     * 写 sysfs 节点。
     *
     * 必须用 FileOutputStream：它对已存在文件以 O_WRONLY 打开，不会带 O_TRUNC；
     * 而 Kotlin 的 File.writeText / FileWriter 会走 O_TRUNC，sysfs 属性节点一律拒绝。
     * FileOutputStream 的 write 会触发 sysfs store 回调，等价于 shell 里的 `echo > node`。
     */
    private fun nativeWriteFile(path: String, value: String) {
        java.io.FileOutputStream(path).use { output ->
            output.write(value.toByteArray())
            output.flush()
        }
    }


    private fun feedback(
        context: Context,
        action: ActionType,
        settings: AppSettings,
        cycleMode: Int?,
        vibrationEnabled: Boolean
    ) {
        if (vibrationEnabled) {
            if (action == ActionType.CYCLE_RINGER && cycleMode != null) {
                vibrateRingerMode(context, cycleMode)
            } else {
                vibrateInstant(context)
            }
        }
        if (settings.toastEnabled) showToast(context, settings.toastText.ifBlank { action.title })
    }

    private fun vibrateRingerMode(context: Context, mode: Int) {
        when (mode) {
            RINGER_MODE_NORMAL -> vibrateDuration(context, 200L)
            RINGER_MODE_VIBRATE -> repeatTapVibration(context, 3)
            RINGER_MODE_SILENT -> vibrateInstant(context)
            else -> vibrateInstant(context)
        }
    }

    private fun repeatTapVibration(context: Context, count: Int) {
        val handler = Handler(Looper.getMainLooper())
        repeat(count) { index ->
            handler.postDelayed({ vibrateInstant(context) }, index * 100L)
        }
    }

    private fun vibrateDuration(context: Context, durationMs: Long) {
        runCatching {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() != true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }.onXposedFailure("vibrate duration")
    }

    private fun vibrateInstant(context: Context) {
        runCatching {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() != true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10L)
            }
        }.onXposedFailure("vibrate")
    }

    private fun showToast(context: Context, message: String) {
        HandlerBridge.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
}
