package com.lingxikey.custom.model

/** Per-application gesture overrides. When the foreground package matches [packageName],
 *  its actions win over the global configuration. A null action means "fall back to global". */
data class AppProfile(
    val packageName: String = "",
    val label: String = "",
    val enabled: Boolean = true,
    val singleAction: ActionType? = null,
    val singleCustom: CustomActionSettings = CustomActionSettings(),
    val doubleAction: ActionType? = null,
    val doubleCustom: CustomActionSettings = CustomActionSettings(),
    val longAction: ActionType? = null,
    val longCustom: CustomActionSettings = CustomActionSettings()
) {
    fun actionFor(gesture: Gesture): ActionType? = when (gesture) {
        Gesture.SINGLE -> singleAction
        Gesture.DOUBLE -> doubleAction
        Gesture.LONG -> longAction
    }

    fun customFor(gesture: Gesture): CustomActionSettings = when (gesture) {
        Gesture.SINGLE -> singleCustom
        Gesture.DOUBLE -> doubleCustom
        Gesture.LONG -> longCustom
    }
}

enum class Gesture(val title: String) {
    SINGLE("单击"),
    DOUBLE("双击"),
    LONG("长按")
}
