package com.lingxikey.custom.model

const val DEFAULT_UNKNOWN_MORSE_TOAST = "未知操作序列"

/**
 * 灵犀键固定按键码（KEY_MACRO17）。本模块只适配灵犀键，按键码写死在程序里，
 * 不再作为可配置项暴露给用户（旧版本的 key_code 配置键会被直接忽略）。
 */
const val SIDE_KEY_CODE = 672

/**
 * 灵犀键固定输入设备节点。仅在「策略 hook 不可用」的原始输入兜底路径中使用，
 * 同样写死在程序里，不做用户配置。
 */
const val SIDE_KEY_DEVICE_PATH = "/dev/input/event2"

data class AppSettings(
    val enabled: Boolean = true,
    val longPressMs: Long = 300L,
    val doubleClickWindowMs: Long = 300L,
    val singleAction: ActionType = ActionType.CYCLE_RINGER,
    val singleWakeScreen: Boolean = true,
    val singleVibrationEnabled: Boolean = false,
    val doubleAction: ActionType = ActionType.NONE,
    val doubleWakeScreen: Boolean = false,
    val doubleVibrationEnabled: Boolean = false,
    val longAction: ActionType = ActionType.SCREENSHOT,
    val longWakeScreen: Boolean = false,
    val longVibrationEnabled: Boolean = false,
    val operationMode: OperationMode = OperationMode.SIMPLE,
    val morseLongPressMs: Long = 300L,
    val morseCommandWindowMs: Long = 300L,
    val morsePressVibrationEnabled: Boolean = false,
    val morseLongVibrationEnabled: Boolean = true,
    val morseImmediateExecutionEnabled: Boolean = true,
    val morseBindings: List<MorseBinding> = emptyList(),
    val appProfiles: List<AppProfile> = emptyList(),
    val singleCustom: CustomActionSettings = CustomActionSettings(),
    val doubleCustom: CustomActionSettings = CustomActionSettings(),
    val longCustom: CustomActionSettings = CustomActionSettings(),
    val vibrationEnabled: Boolean = true,
    val toastEnabled: Boolean = false,
    val toastText: String = "侧键操作已执行",
    val unknownMorseFeedbackEnabled: Boolean = false,
    val unknownMorseToastText: String = DEFAULT_UNKNOWN_MORSE_TOAST,
    val wakeScreenWhenOff: Boolean = false,
    val cursorControlMode: CursorControlMode = CursorControlMode.DISABLED,
    val cursorLongPressAction: CursorLongPressAction = CursorLongPressAction.NONE,
    val cursorLongPressMs: Long = DEFAULT_CURSOR_LONG_PRESS_MS,
    val cursorRepeatIntervalMs: Long = DEFAULT_CURSOR_REPEAT_INTERVAL_MS,
    val combinationEnabled: Boolean = false,
    val sideVolumeUpAction: SideKeyCombinationAction = SideKeyCombinationAction.NONE,
    val sideVolumeDownAction: SideKeyCombinationAction = SideKeyCombinationAction.NONE,
    val sidePowerAction: SideKeyCombinationAction = SideKeyCombinationAction.NONE
) {
    val hasConfiguredCombination: Boolean
        get() = combinationEnabled && listOf(sideVolumeUpAction, sideVolumeDownAction, sidePowerAction)
            .any { it != SideKeyCombinationAction.NONE }
}
