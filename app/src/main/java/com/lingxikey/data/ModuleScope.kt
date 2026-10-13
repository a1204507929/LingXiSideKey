package com.lingxikey.custom.data

/**
 * LSPosed / Vector 作用域（模块生效范围）的声明与校验。
 *
 * 模块的 hook 点分布在三个进程里，任何一个漏勾选都会让对应功能静默失效：
 *   - android                 系统框架（system_server）：灵犀键事件拦截，必需，否则整个模块不工作
 *   - com.oplus.camera        相机进程：快门 / 录像按钮注入，用于「相机：拍照 / 录制」
 *   - com.coloros.shortcuts   ColorOS 快捷指令进程：小布快捷指令直连
 *
 * 作用域通过 libxposed service 102 的 XposedService.getScope() / requestScope() 读写，
 * 由框架（Vector / LSPosed 管理器）负责弹窗或直接应用。
 */
object ModuleScope {

    const val SYSTEM_FRAMEWORK = "android"
    const val CAMERA = "com.oplus.camera"
    const val SHORTCUTS = "com.coloros.shortcuts"

    data class Entry(
        /** 作用域包名。 */
        val packageName: String,
        /** 界面展示名。 */
        val label: String,
        /** 该作用域负责的功能说明。 */
        val description: String,
        /** true = 不勾选模块完全不工作；false = 只影响对应功能。 */
        val required: Boolean
    )

    val entries: List<Entry> = listOf(
        Entry(
            SYSTEM_FRAMEWORK,
            "系统框架",
            "灵犀键按下 / 单击 / 双击 / 长按的拦截与分发",
            required = true
        ),
        Entry(
            CAMERA,
            "相机",
            "相机内的拍照 / 录像按键注入",
            required = false
        ),
        Entry(
            SHORTCUTS,
            "快捷指令",
            "小布快捷指令的执行通道",
            required = false
        )
    )

    /** 全部建议勾选的作用域包名。 */
    val allPackages: List<String> = entries.map { it.packageName }

    /** 必需但缺失的作用域（模块当前不生效时的根因）。 */
    fun missingRequired(scope: Collection<String>): List<String> =
        entries.filter { it.required && it.packageName !in scope }.map { it.packageName }

    /** 未勾选的作用域包名（含可选）。 */
    fun missing(scope: Collection<String>): List<String> =
        entries.filter { it.packageName !in scope }.map { it.packageName }

    fun isChecked(scope: Collection<String>, packageName: String): Boolean = packageName in scope
}
