package com.lingxikey.custom.model

data class CustomActionSettings(
    val commonAction: CommonAction = CommonAction.WECHAT_PAY,
    /** 「打开应用」动作选择的包名与显示名（与 CUSTOM_ACTIVITY 的字段互不干扰）。 */
    val launchAppPackage: String = "",
    val launchAppLabel: String = "",
    val activityPackage: String = "",
    val activityClass: String = "",
    val activityAction: String = "",
    val launchInSmallWindow: Boolean = false,
    val urlScheme: String = "",
    val xiaobuShortcutId: String = "",
    val shellCommand: String = "",
    val shellToastEnabled: Boolean = true
)
