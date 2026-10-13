package com.lingxikey.custom.data

import android.content.Context
import android.content.SharedPreferences
import android.os.ParcelFileDescriptor
import com.lingxikey.custom.onXposedFailure
import com.lingxikey.custom.xposed.XposedBridge
import com.lingxikey.custom.model.ActionType
import com.lingxikey.custom.model.AppProfile
import com.lingxikey.custom.model.AppSettings
import com.lingxikey.custom.model.CommonAction
import com.lingxikey.custom.model.CustomActionSettings
import com.lingxikey.custom.model.CursorControlMode
import com.lingxikey.custom.model.CursorLongPressAction
import com.lingxikey.custom.model.DEFAULT_CURSOR_LONG_PRESS_MS
import com.lingxikey.custom.model.DEFAULT_CURSOR_REPEAT_INTERVAL_MS
import com.lingxikey.custom.model.MAX_CURSOR_LONG_PRESS_MS
import com.lingxikey.custom.model.MAX_CURSOR_REPEAT_INTERVAL_MS
import com.lingxikey.custom.model.MIN_CURSOR_LONG_PRESS_MS
import com.lingxikey.custom.model.MIN_CURSOR_REPEAT_INTERVAL_MS
import com.lingxikey.custom.model.DEFAULT_UNKNOWN_MORSE_TOAST
import com.lingxikey.custom.model.MorseBinding
import com.lingxikey.custom.model.OperationMode
import com.lingxikey.custom.model.SideKeyCombinationAction
import com.lingxikey.custom.model.SIDE_KEY_CODE
import com.lingxikey.custom.model.SIDE_KEY_DEVICE_PATH
import io.github.libxposed.service.XposedService
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

/** Stores a complete settings snapshot in LibXposed's shared remote file. */
object SettingsStore {
    const val PREFS_NAME = "settings"
    const val REMOTE_FILE_NAME = "settings.json"

    /** Camera profile is restricted to 拍照 / 录制 only. */
    const val CAMERA_PACKAGE = "com.oplus.camera"

    /**
     * 已永久移除的动作名（改用「打开应用」实现）。
     * 旧配置里读到这些名字时降级为「未配置」，避免退化成默认动作。
     */
    private val RETIRED_ACTIONS = setOf("CAMERA", "XIAOBU_ASSISTANT")

    private val remoteService: XposedService?
        get() = XposedServiceManager.xposedService

    @Volatile
    private var pendingRemoteSettings: AppSettings? = null
    @Volatile
    private var applicationContext: Context? = null
    @Volatile
    private var remoteSyncInitialized = false

    private const val KEY_ENABLED = "enabled"
    private const val KEY_LONG_PRESS_MS = "long_press_ms"
    private const val KEY_DOUBLE_WINDOW_MS = "double_click_window_ms"
    private const val KEY_SINGLE_ACTION = "single_action"
    private const val KEY_SINGLE_WAKE_SCREEN = "single_wake_screen"
    private const val KEY_SINGLE_VIBRATION = "single_vibration_enabled"
    private const val KEY_DOUBLE_ACTION = "double_action"
    private const val KEY_DOUBLE_WAKE_SCREEN = "double_wake_screen"
    private const val KEY_DOUBLE_VIBRATION = "double_vibration_enabled"
    private const val KEY_LONG_ACTION = "long_action"
    private const val KEY_LONG_WAKE_SCREEN = "long_wake_screen"
    private const val KEY_LONG_VIBRATION = "long_vibration_enabled"
    private const val KEY_OPERATION_MODE = "operation_mode"
    private const val KEY_MORSE_LONG_PRESS_MS = "morse_long_press_ms"
    private const val KEY_MORSE_COMMAND_WINDOW_MS = "morse_command_window_ms"
    private const val KEY_MORSE_PRESS_VIBRATION = "morse_press_vibration"
    private const val KEY_MORSE_LONG_VIBRATION = "morse_long_vibration"
    private const val KEY_MORSE_IMMEDIATE_EXECUTION = "morse_immediate_execution"
    private const val KEY_MORSE_BINDINGS = "morse_bindings"
    private const val KEY_APP_PROFILES = "app_profiles"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_TOAST_ENABLED = "toast_enabled"
    private const val KEY_TOAST_TEXT = "toast_text"
    private const val KEY_UNKNOWN_MORSE_FEEDBACK = "unknown_morse_feedback"
    private const val KEY_UNKNOWN_MORSE_TOAST_TEXT = "unknown_morse_toast_text"
    private const val KEY_WAKE_SCREEN = "wake_screen_when_off"
    private const val KEY_CURSOR_CONTROL_MODE = "cursor_control_mode"
    private const val KEY_CURSOR_LONG_PRESS_ACTION = "cursor_long_press_action"
    private const val KEY_CURSOR_LONG_PRESS_MS = "cursor_long_press_ms"
    private const val KEY_CURSOR_REPEAT_INTERVAL_MS = "cursor_repeat_interval_ms"
    private const val KEY_COMBINATION_ENABLED = "combination_enabled"
    private const val KEY_SIDE_VOLUME_UP_ACTION = "side_volume_up_action"
    private const val KEY_SIDE_VOLUME_DOWN_ACTION = "side_volume_down_action"
    private const val KEY_SIDE_POWER_ACTION = "side_power_action"

    @Synchronized
    fun initializeRemoteStorage(context: Context? = null) {
        context?.let { applicationContext = it.applicationContext }
        if (remoteSyncInitialized) return
        remoteSyncInitialized = true
        XposedServiceManager.registerBindListener { service ->
            val context = applicationContext ?: return@registerBindListener
            val localPreferences = preferences(context)
            val settings = pendingRemoteSettings
                ?: if (localPreferences.all.isNotEmpty()) {
                    fromPreferences(localPreferences)
                } else {
                    migrateEmptyLocalSettings(service, context)
                }
                ?: return@registerBindListener
            if (pendingRemoteSettings != null) writeLocal(context, settings)
            if (writeRemote(service, settings)) pendingRemoteSettings = null
        }
    }

    fun load(context: Context): AppSettings {
        initializeRemoteStorage(context)
        val localPreferences = preferences(context)
        val settings = fromPreferences(localPreferences)
        if (localPreferences.all.isNotEmpty() || remoteService != null) {
            if (!writeLocal(context, settings) || !writeRemote(settings)) {
                pendingRemoteSettings = settings
            } else {
                pendingRemoteSettings = null
            }
        }
        return settings
    }

    fun save(settings: AppSettings) {
        initializeRemoteStorage()
        val context = applicationContext
        if (context == null || !writeLocal(context, settings)) {
            pendingRemoteSettings = settings
            return
        }
        if (writeRemote(settings)) pendingRemoteSettings = null else pendingRemoteSettings = settings
    }

    private fun migrateEmptyLocalSettings(service: XposedService, context: Context): AppSettings? {
        return try {
            readRemote(service)?.let { settings ->
                writeLocal(context, settings)
                return settings
            }

            // Preserve settings written by the previous 102 preference implementation.
            val oldRemote = runCatching { service.getRemotePreferences(PREFS_NAME) }
                .onXposedFailure("read legacy remote preferences")
                .getOrNull()
            if (oldRemote?.all?.isNotEmpty() == true) {
                return fromPreferences(oldRemote).also { writeLocal(context, it) }
            }

            fromPreferences(preferences(context)).also { writeLocal(context, it) }
        } catch (error: Throwable) {
            XposedBridge.log("LingXiKeyCustom: migrate existing settings failed: ${error.message}")
            XposedBridge.log(error)
            null
        }
    }

    private fun writeRemote(settings: AppSettings): Boolean = remoteService?.let { writeRemote(it, settings) } == true

    private fun writeRemote(service: XposedService, settings: AppSettings): Boolean = runCatching {
        val descriptor = service.openRemoteFile(REMOTE_FILE_NAME)
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
            output.channel.truncate(0)
            output.write(toJson(settings).toString().toByteArray(StandardCharsets.UTF_8))
            output.channel.force(true)
        }

        XposedBridge.log("LingXiKeyCustom: remote settings file saved.")
        true
    }.onXposedFailure("save remote settings file").getOrDefault(false)

    private fun readRemote(service: XposedService?): AppSettings? {
        if (service == null) {
            XposedBridge.log("LingXiKeyCustom: service not initialized, can not read remote settings")
            return null
        }
        return try {

            val descriptor = service.openRemoteFile(REMOTE_FILE_NAME)
            if (descriptor.statSize <= 0L) {
                descriptor.close()
                XposedBridge.log("LingXiKeyCustom: remote settings file is empty.")
                null
            } else {
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                    fromJson(JSONObject(input.readBytes().toString(StandardCharsets.UTF_8)))
                }
            }
        } catch (_: FileNotFoundException) {
            XposedBridge.log("LingXiKeyCustom: remote settings file not found.")
            null
        } catch (error: Throwable) {
            XposedBridge.log("LingXiKeyCustom: load remote settings file failed: ${error.message}")
            XposedBridge.log(error)
            null
        }
    }

    private fun preferences(context: Context): SharedPreferences = context
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun writeLocal(context: Context, settings: AppSettings): Boolean = runCatching {
        preferences(context).edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putLong(KEY_LONG_PRESS_MS, settings.longPressMs)
            .putLong(KEY_DOUBLE_WINDOW_MS, settings.doubleClickWindowMs)
            .putString(KEY_SINGLE_ACTION, settings.singleAction.name)
            .putBoolean(KEY_SINGLE_WAKE_SCREEN, settings.singleWakeScreen)
            .putBoolean(KEY_SINGLE_VIBRATION, settings.singleVibrationEnabled)
            .putString(KEY_DOUBLE_ACTION, settings.doubleAction.name)
            .putBoolean(KEY_DOUBLE_WAKE_SCREEN, settings.doubleWakeScreen)
            .putBoolean(KEY_DOUBLE_VIBRATION, settings.doubleVibrationEnabled)
            .putString(KEY_LONG_ACTION, settings.longAction.name)
            .putBoolean(KEY_LONG_WAKE_SCREEN, settings.longWakeScreen)
            .putBoolean(KEY_LONG_VIBRATION, settings.longVibrationEnabled)
            .putString(KEY_OPERATION_MODE, settings.operationMode.name)
            .putLong(KEY_MORSE_LONG_PRESS_MS, settings.morseLongPressMs)
            .putLong(KEY_MORSE_COMMAND_WINDOW_MS, settings.morseCommandWindowMs)
            .putBoolean(KEY_MORSE_PRESS_VIBRATION, settings.morsePressVibrationEnabled)
            .putBoolean(KEY_MORSE_LONG_VIBRATION, settings.morseLongVibrationEnabled)
            .putBoolean(KEY_MORSE_IMMEDIATE_EXECUTION, settings.morseImmediateExecutionEnabled)
            .putString(KEY_MORSE_BINDINGS, writeMorseBindings(settings.morseBindings))
            .putString(KEY_APP_PROFILES, writeAppProfiles(settings.appProfiles))
            .also { writeCustom(it, "single_", settings.singleCustom) }
            .also { writeCustom(it, "double_", settings.doubleCustom) }
            .also { writeCustom(it, "long_", settings.longCustom) }
            .putBoolean(KEY_VIBRATION_ENABLED, settings.vibrationEnabled)
            .putBoolean(KEY_TOAST_ENABLED, settings.toastEnabled)
            .putString(KEY_TOAST_TEXT, settings.toastText)
            .putBoolean(KEY_UNKNOWN_MORSE_FEEDBACK, settings.unknownMorseFeedbackEnabled)
            .putString(KEY_UNKNOWN_MORSE_TOAST_TEXT, settings.unknownMorseToastText)
            .putBoolean(KEY_WAKE_SCREEN, settings.wakeScreenWhenOff)
            .putString(KEY_CURSOR_CONTROL_MODE, settings.cursorControlMode.name)
            .putString(KEY_CURSOR_LONG_PRESS_ACTION, settings.cursorLongPressAction.name)
            .putLong(KEY_CURSOR_LONG_PRESS_MS, settings.cursorLongPressMs)
            .putLong(KEY_CURSOR_REPEAT_INTERVAL_MS, settings.cursorRepeatIntervalMs)
            .putBoolean(KEY_COMBINATION_ENABLED, settings.combinationEnabled)
            .putString(KEY_SIDE_VOLUME_UP_ACTION, settings.sideVolumeUpAction.name)
            .putString(KEY_SIDE_VOLUME_DOWN_ACTION, settings.sideVolumeDownAction.name)
            .putString(KEY_SIDE_POWER_ACTION, settings.sidePowerAction.name)
            .commit()
    }.onXposedFailure("save local settings").getOrDefault(false)

    /**
     * Features removed from the UI (Morse mode, key combinations, IME cursor control, wake-on-execute
     * and wake-when-off) are pinned to their disabled defaults here, so any values left over from
     * older versions can never re-enable them.
     */
    fun fromPreferences(preferences: SharedPreferences): AppSettings = AppSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, true),
        longPressMs = preferences.getLong(KEY_LONG_PRESS_MS, 300L).coerceIn(100L, 800L),
        doubleClickWindowMs = preferences.getLong(KEY_DOUBLE_WINDOW_MS, 300L).coerceIn(100L, 800L),
        singleAction = action(preferences.getString(KEY_SINGLE_ACTION, null)),
        singleWakeScreen = false,
        singleVibrationEnabled = preferences.getBoolean(KEY_SINGLE_VIBRATION, false),
        doubleAction = action(preferences.getString(KEY_DOUBLE_ACTION, null), ActionType.NONE),
        doubleWakeScreen = false,
        doubleVibrationEnabled = preferences.getBoolean(KEY_DOUBLE_VIBRATION, false),
        longAction = action(preferences.getString(KEY_LONG_ACTION, null), ActionType.SCREENSHOT),
        longWakeScreen = false,
        longVibrationEnabled = preferences.getBoolean(KEY_LONG_VIBRATION, false),
        operationMode = OperationMode.SIMPLE,
        morseLongPressMs = preferences.getLong(KEY_MORSE_LONG_PRESS_MS, 300L).coerceIn(100L, 800L),
        morseCommandWindowMs = preferences.getLong(KEY_MORSE_COMMAND_WINDOW_MS, 300L).coerceIn(100L, 800L),
        morsePressVibrationEnabled = preferences.getBoolean(KEY_MORSE_PRESS_VIBRATION, false),
        morseLongVibrationEnabled = preferences.getBoolean(KEY_MORSE_LONG_VIBRATION, true),
        morseImmediateExecutionEnabled = preferences.getBoolean(KEY_MORSE_IMMEDIATE_EXECUTION, true),
        morseBindings = readMorseBindings(preferences.getString(KEY_MORSE_BINDINGS, null)),
        appProfiles = readAppProfiles(preferences.getString(KEY_APP_PROFILES, null)),
        singleCustom = readCustom(preferences, "single_"),
        doubleCustom = readCustom(preferences, "double_"),
        longCustom = readCustom(preferences, "long_"),
        vibrationEnabled = preferences.getBoolean(KEY_VIBRATION_ENABLED, true),
        toastEnabled = preferences.getBoolean(KEY_TOAST_ENABLED, false),
        toastText = preferences.getString(KEY_TOAST_TEXT, "侧键操作已执行") ?: "侧键操作已执行",
        unknownMorseFeedbackEnabled = false,
        unknownMorseToastText = preferences.getString(KEY_UNKNOWN_MORSE_TOAST_TEXT, DEFAULT_UNKNOWN_MORSE_TOAST)
            ?: DEFAULT_UNKNOWN_MORSE_TOAST,
        wakeScreenWhenOff = false,
        cursorControlMode = CursorControlMode.DISABLED,
        cursorLongPressAction = CursorLongPressAction.NONE,
        cursorLongPressMs = preferences.getLong(KEY_CURSOR_LONG_PRESS_MS, DEFAULT_CURSOR_LONG_PRESS_MS)
            .coerceIn(MIN_CURSOR_LONG_PRESS_MS, MAX_CURSOR_LONG_PRESS_MS),
        cursorRepeatIntervalMs = preferences.getLong(KEY_CURSOR_REPEAT_INTERVAL_MS, DEFAULT_CURSOR_REPEAT_INTERVAL_MS)
            .coerceIn(MIN_CURSOR_REPEAT_INTERVAL_MS, MAX_CURSOR_REPEAT_INTERVAL_MS),
        combinationEnabled = false,
        sideVolumeUpAction = SideKeyCombinationAction.NONE,
        sideVolumeDownAction = SideKeyCombinationAction.NONE,
        sidePowerAction = SideKeyCombinationAction.NONE
    )

    /** Serialize the complete settings snapshot used by the LibXposed remote-file bridge. */
    internal fun toJson(settings: AppSettings): JSONObject = JSONObject().apply {
        put(KEY_ENABLED, settings.enabled)
        put(KEY_LONG_PRESS_MS, settings.longPressMs)
        put(KEY_DOUBLE_WINDOW_MS, settings.doubleClickWindowMs)
        put(KEY_SINGLE_ACTION, settings.singleAction.name)
        put(KEY_SINGLE_WAKE_SCREEN, settings.singleWakeScreen)
        put(KEY_SINGLE_VIBRATION, settings.singleVibrationEnabled)
        put(KEY_DOUBLE_ACTION, settings.doubleAction.name)
        put(KEY_DOUBLE_WAKE_SCREEN, settings.doubleWakeScreen)
        put(KEY_DOUBLE_VIBRATION, settings.doubleVibrationEnabled)
        put(KEY_LONG_ACTION, settings.longAction.name)
        put(KEY_LONG_WAKE_SCREEN, settings.longWakeScreen)
        put(KEY_LONG_VIBRATION, settings.longVibrationEnabled)
        put(KEY_OPERATION_MODE, settings.operationMode.name)
        put(KEY_MORSE_LONG_PRESS_MS, settings.morseLongPressMs)
        put(KEY_MORSE_COMMAND_WINDOW_MS, settings.morseCommandWindowMs)
        put(KEY_MORSE_PRESS_VIBRATION, settings.morsePressVibrationEnabled)
        put(KEY_MORSE_LONG_VIBRATION, settings.morseLongVibrationEnabled)
        put(KEY_MORSE_IMMEDIATE_EXECUTION, settings.morseImmediateExecutionEnabled)
        put(KEY_MORSE_BINDINGS, JSONArray(writeMorseBindings(settings.morseBindings)))
        put(KEY_APP_PROFILES, JSONArray(writeAppProfiles(settings.appProfiles)))
        put("single_custom", customToJson(settings.singleCustom))
        put("double_custom", customToJson(settings.doubleCustom))
        put("long_custom", customToJson(settings.longCustom))
        put(KEY_VIBRATION_ENABLED, settings.vibrationEnabled)
        put(KEY_TOAST_ENABLED, settings.toastEnabled)
        put(KEY_TOAST_TEXT, settings.toastText)
        put(KEY_UNKNOWN_MORSE_FEEDBACK, settings.unknownMorseFeedbackEnabled)
        put(KEY_UNKNOWN_MORSE_TOAST_TEXT, settings.unknownMorseToastText)
        put(KEY_WAKE_SCREEN, settings.wakeScreenWhenOff)
        put(KEY_CURSOR_CONTROL_MODE, settings.cursorControlMode.name)
        put(KEY_CURSOR_LONG_PRESS_ACTION, settings.cursorLongPressAction.name)
        put(KEY_CURSOR_LONG_PRESS_MS, settings.cursorLongPressMs)
        put(KEY_CURSOR_REPEAT_INTERVAL_MS, settings.cursorRepeatIntervalMs)
        put(KEY_COMBINATION_ENABLED, settings.combinationEnabled)
        put(KEY_SIDE_VOLUME_UP_ACTION, settings.sideVolumeUpAction.name)
        put(KEY_SIDE_VOLUME_DOWN_ACTION, settings.sideVolumeDownAction.name)
        put(KEY_SIDE_POWER_ACTION, settings.sidePowerAction.name)
    }

    /** Deserialize a remote-file snapshot, applying the same bounds and defaults as preferences. */
    internal fun fromJson(json: JSONObject): AppSettings = AppSettings(
        enabled = json.optBoolean(KEY_ENABLED, true),
        longPressMs = json.optLong(KEY_LONG_PRESS_MS, 300L).coerceIn(100L, 800L),
        doubleClickWindowMs = json.optLong(KEY_DOUBLE_WINDOW_MS, 300L).coerceIn(100L, 800L),
        singleAction = action(optionalString(json, KEY_SINGLE_ACTION)),
        singleWakeScreen = false,
        singleVibrationEnabled = json.optBoolean(KEY_SINGLE_VIBRATION, false),
        doubleAction = action(optionalString(json, KEY_DOUBLE_ACTION), ActionType.NONE),
        doubleWakeScreen = false,
        doubleVibrationEnabled = json.optBoolean(KEY_DOUBLE_VIBRATION, false),
        longAction = action(optionalString(json, KEY_LONG_ACTION), ActionType.SCREENSHOT),
        longWakeScreen = false,
        longVibrationEnabled = json.optBoolean(KEY_LONG_VIBRATION, false),
        operationMode = OperationMode.SIMPLE,
        morseLongPressMs = json.optLong(KEY_MORSE_LONG_PRESS_MS, 300L).coerceIn(100L, 800L),
        morseCommandWindowMs = json.optLong(KEY_MORSE_COMMAND_WINDOW_MS, 300L).coerceIn(100L, 800L),
        morsePressVibrationEnabled = json.optBoolean(KEY_MORSE_PRESS_VIBRATION, false),
        morseLongVibrationEnabled = json.optBoolean(KEY_MORSE_LONG_VIBRATION, true),
        morseImmediateExecutionEnabled = json.optBoolean(KEY_MORSE_IMMEDIATE_EXECUTION, true),
        morseBindings = readMorseBindings(json.optJSONArray(KEY_MORSE_BINDINGS)?.toString()),
        appProfiles = readAppProfiles(json.optJSONArray(KEY_APP_PROFILES)?.toString()),
        singleCustom = customFromJson(json.optJSONObject("single_custom")),
        doubleCustom = customFromJson(json.optJSONObject("double_custom")),
        longCustom = customFromJson(json.optJSONObject("long_custom")),
        vibrationEnabled = json.optBoolean(KEY_VIBRATION_ENABLED, true),
        toastEnabled = json.optBoolean(KEY_TOAST_ENABLED, false),
        toastText = json.optString(KEY_TOAST_TEXT, "侧键操作已执行"),
        unknownMorseFeedbackEnabled = false,
        unknownMorseToastText = json.optString(KEY_UNKNOWN_MORSE_TOAST_TEXT, DEFAULT_UNKNOWN_MORSE_TOAST),
        wakeScreenWhenOff = false,
        cursorControlMode = CursorControlMode.DISABLED,
        cursorLongPressAction = CursorLongPressAction.NONE,
        cursorLongPressMs = json.optLong(KEY_CURSOR_LONG_PRESS_MS, DEFAULT_CURSOR_LONG_PRESS_MS)
            .coerceIn(MIN_CURSOR_LONG_PRESS_MS, MAX_CURSOR_LONG_PRESS_MS),
        cursorRepeatIntervalMs = json.optLong(KEY_CURSOR_REPEAT_INTERVAL_MS, DEFAULT_CURSOR_REPEAT_INTERVAL_MS)
            .coerceIn(MIN_CURSOR_REPEAT_INTERVAL_MS, MAX_CURSOR_REPEAT_INTERVAL_MS),
        combinationEnabled = false,
        sideVolumeUpAction = SideKeyCombinationAction.NONE,
        sideVolumeDownAction = SideKeyCombinationAction.NONE,
        sidePowerAction = SideKeyCombinationAction.NONE
    )

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T, description: String): T =
        value?.let { runCatching { enumValueOf<T>(it) }.onXposedFailure("parse $description").getOrNull() } ?: fallback

    private fun customToJson(custom: CustomActionSettings): JSONObject = JSONObject().apply {
        put("common_action", custom.commonAction.name)
        put("activity_package", custom.activityPackage)
        put("activity_class", custom.activityClass)
        put("activity_action", custom.activityAction)
        put("activity_small_window", custom.launchInSmallWindow)
        put("url_scheme", custom.urlScheme)
        put("xiaobu_shortcut_id", custom.xiaobuShortcutId)
        put("launch_app_package", custom.launchAppPackage)
        put("launch_app_label", custom.launchAppLabel)
        put("shell_command", custom.shellCommand)
        put("shell_toast_enabled", custom.shellToastEnabled)
    }

    private fun customFromJson(json: JSONObject?): CustomActionSettings {
        val value = json ?: JSONObject()
        val common = enumValue(optionalString(value, "common_action"), CommonAction.WECHAT_PAY, "common action")
        return CustomActionSettings(
            commonAction = common,
            activityPackage = value.optString("activity_package", ""),
            activityClass = value.optString("activity_class", ""),
            activityAction = value.optString("activity_action", ""),
            launchInSmallWindow = value.optBoolean("activity_small_window", false),
            urlScheme = value.optString("url_scheme", ""),
            xiaobuShortcutId = value.optString("xiaobu_shortcut_id", ""),
            launchAppPackage = value.optString("launch_app_package", ""),
            launchAppLabel = value.optString("launch_app_label", ""),
            shellCommand = value.optString("shell_command", ""),
            shellToastEnabled = value.optBoolean("shell_toast_enabled", true)
        )
    }

    private fun optionalString(json: JSONObject, key: String): String? =
        if (json.has(key) && !json.isNull(key)) json.optString(key) else null

    private fun action(value: String?, fallback: ActionType = ActionType.CYCLE_RINGER): ActionType {
        if (value == null) return fallback
        // 已移除的动作（打开相机 / 唤起小布助手，改用「打开应用」实现）在旧配置里读到后，
        // 统一降级为「未配置」，避免退化成意料外的响铃切换。
        if (value in RETIRED_ACTIONS) return ActionType.NONE
        return runCatching { ActionType.valueOf(value) }.onXposedFailure("parse action").getOrNull() ?: fallback
    }

    private fun writeCustom(editor: SharedPreferences.Editor, prefix: String, custom: CustomActionSettings) {
        editor.putString("${prefix}common_action", custom.commonAction.name)
            .putString("${prefix}activity_package", custom.activityPackage)
            .putString("${prefix}activity_class", custom.activityClass)
            .putString("${prefix}activity_action", custom.activityAction)
            .putBoolean("${prefix}activity_small_window", custom.launchInSmallWindow)
            .putString("${prefix}url_scheme", custom.urlScheme)
            .putString("${prefix}xiaobu_shortcut_id", custom.xiaobuShortcutId)
            .putString("${prefix}launch_app_package", custom.launchAppPackage)
            .putString("${prefix}launch_app_label", custom.launchAppLabel)
            .putString("${prefix}shell_command", custom.shellCommand)
            .putBoolean("${prefix}shell_toast_enabled", custom.shellToastEnabled)
    }

    private fun readCustom(preferences: SharedPreferences, prefix: String): CustomActionSettings {
        val common = preferences.getString("${prefix}common_action", null)
            ?.let { runCatching { CommonAction.valueOf(it) }.onXposedFailure("parse common action").getOrNull() }
            ?: CommonAction.WECHAT_PAY
        return CustomActionSettings(
            commonAction = common,
            activityPackage = preferences.getString("${prefix}activity_package", "") ?: "",
            activityClass = preferences.getString("${prefix}activity_class", "") ?: "",
            activityAction = preferences.getString("${prefix}activity_action", "") ?: "",
            launchInSmallWindow = preferences.getBoolean("${prefix}activity_small_window", false),
            urlScheme = preferences.getString("${prefix}url_scheme", "") ?: "",
            xiaobuShortcutId = preferences.getString("${prefix}xiaobu_shortcut_id", "") ?: "",
            launchAppPackage = preferences.getString("${prefix}launch_app_package", "") ?: "",
            launchAppLabel = preferences.getString("${prefix}launch_app_label", "") ?: "",
            shellCommand = preferences.getString("${prefix}shell_command", "") ?: "",
            shellToastEnabled = preferences.getBoolean("${prefix}shell_toast_enabled", true)
        )
    }

    private fun writeAppProfiles(profiles: List<AppProfile>): String = JSONArray().apply {
        profiles.forEach { profile ->
            put(JSONObject().apply {
                put("package_name", profile.packageName)
                put("label", profile.label)
                put("enabled", profile.enabled)
                profile.singleAction?.let { put("single_action", it.name) }
                profile.doubleAction?.let { put("double_action", it.name) }
                profile.longAction?.let { put("long_action", it.name) }
                put("single_custom", customToJson(profile.singleCustom))
                put("double_custom", customToJson(profile.doubleCustom))
                put("long_custom", customToJson(profile.longCustom))
            })
        }
    }.toString()

    private fun readAppProfiles(value: String?): List<AppProfile> {
        val array = runCatching { JSONArray(value ?: "[]") }
            .onXposedFailure("parse app profiles JSON")
            .getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val entry = array.optJSONObject(index) ?: return@mapNotNull null
            val packageName = entry.optString("package_name")
            if (packageName.isBlank()) return@mapNotNull null
            AppProfile(
                packageName = packageName,
                label = entry.optString("label"),
                enabled = entry.optBoolean("enabled", true),
                singleAction = sanitizeProfileAction(packageName, optionalAction(entry, "single_action")),
                doubleAction = sanitizeProfileAction(packageName, optionalAction(entry, "double_action")),
                longAction = sanitizeProfileAction(packageName, optionalAction(entry, "long_action")),
                singleCustom = customFromJson(entry.optJSONObject("single_custom")) ?: CustomActionSettings(),
                doubleCustom = customFromJson(entry.optJSONObject("double_custom")) ?: CustomActionSettings(),
                longCustom = customFromJson(entry.optJSONObject("long_custom")) ?: CustomActionSettings()
            )
        }
    }

    /**
     * The camera profile only offers 拍照 / 录制 (CAMERA_SHUTTER) plus "未设置". Any other action
     * left over from an older version is dropped back to null (follow the global gesture).
     */
    internal fun sanitizeProfileAction(packageName: String, action: ActionType?): ActionType? {
        if (packageName != CAMERA_PACKAGE) return action
        return when (action) {
            null, ActionType.NONE, ActionType.CAMERA_SHUTTER -> action
            else -> null
        }
    }

    private fun optionalAction(entry: JSONObject, key: String): ActionType? {
        val raw = entry.optString(key)
        if (raw.isBlank()) return null
        if (raw in RETIRED_ACTIONS) return ActionType.NONE
        return runCatching { ActionType.valueOf(raw) }.onXposedFailure("parse app profile action").getOrNull()
    }

    private fun writeMorseBindings(bindings: List<MorseBinding>): String = JSONArray().apply {
        bindings.forEach { binding ->
            put(JSONObject().apply {
                put("sequence", binding.sequence)
                put("action", binding.action.name)
                put("custom", JSONObject().apply {
                    put("common_action", binding.custom.commonAction.name)
                    put("activity_package", binding.custom.activityPackage)
                    put("activity_class", binding.custom.activityClass)
                    put("activity_action", binding.custom.activityAction)
                    put("activity_small_window", binding.custom.launchInSmallWindow)
                    put("url_scheme", binding.custom.urlScheme)
                    put("xiaobu_shortcut_id", binding.custom.xiaobuShortcutId)
                    put("shell_command", binding.custom.shellCommand)
                    put("shell_toast_enabled", binding.custom.shellToastEnabled)
                })
                put("wake_screen", binding.wakeScreen)
                put("vibration_enabled", binding.vibrationEnabled)
            })
        }
    }.toString()

    private fun readMorseBindings(value: String?): List<MorseBinding> {
        val array = runCatching { JSONArray(value ?: "[]") }
            .onXposedFailure("parse Morse bindings JSON")
            .getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val entry = array.optJSONObject(index) ?: return@mapNotNull null
            val sequence = entry.optString("sequence")
            if (sequence.isEmpty() || sequence.any { it != '0' && it != '1' }) return@mapNotNull null
            val action = runCatching { ActionType.valueOf(entry.optString("action")) }
                .onXposedFailure("parse Morse action")
                .getOrNull()
                ?: return@mapNotNull null
            val custom = entry.optJSONObject("custom") ?: JSONObject()
            val common = runCatching { CommonAction.valueOf(custom.optString("common_action")) }
                .onXposedFailure("parse Morse common action")
                .getOrNull()
                ?: CommonAction.WECHAT_PAY
            MorseBinding(
                sequence = sequence,
                action = action,
                custom = CustomActionSettings(
                    commonAction = common,
                    activityPackage = custom.optString("activity_package"),
                    activityClass = custom.optString("activity_class"),
                    activityAction = custom.optString("activity_action"),
                    launchInSmallWindow = custom.optBoolean("activity_small_window", false),
                    urlScheme = custom.optString("url_scheme"),
                    xiaobuShortcutId = custom.optString("xiaobu_shortcut_id"),
                    shellCommand = custom.optString("shell_command"),
                    shellToastEnabled = custom.optBoolean("shell_toast_enabled", true)
                ),
                wakeScreen = entry.optBoolean("wake_screen", action == ActionType.CYCLE_RINGER),
                vibrationEnabled = entry.optBoolean("vibration_enabled", false)
            )
        }.distinctBy { it.sequence }
    }
}
