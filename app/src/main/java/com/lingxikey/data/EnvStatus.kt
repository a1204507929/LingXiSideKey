package com.lingxikey.custom.data

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 运行环境探测：root（KernelSU）与 Xposed 框架（Vector / LSPosed）的安装与激活状态，
 * 以及对应的跳转 / 修复动作。
 *
 * 本机的 root 链路是「KernelSU 晚加载 + GhostLock 激活」：开机后 root 并不可用，
 * 要等 GhostLock 拉起后才挂载 su，因此 su 节点是否存在就是「root 是否已激活」的直接信号。
 */
object EnvStatus {

    /** su 二进制的常见位置。KSU 激活（GhostLock 晚加载）后才会挂载出来。 */
    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/debug_ramdisk/su",
        "/data/adb/ksu/bin/su"
    )

    const val VECTOR_PACKAGE = "org.matrix.vector.manager"
    const val LSPOSED_PACKAGE = "org.lsposed.manager"

    /** 已知框架管理器（按优先级）：Vector 在前，LSPosed 兜底。 */
    val FRAMEWORK_PACKAGES = listOf(VECTOR_PACKAGE, LSPOSED_PACKAGE)

    const val GHOSTLOCK_PACKAGE = "com.ghostlock.app"
    const val GHOSTLOCK_ACTIVITY = "com.ghostlock.app.ui.MainActivity"

    /** ksud 全路径（Runtime.exec 不解析 PATH，必须用绝对路径）。 */
    private const val KSUD = "/data/adb/ksu/bin/ksud"

    /**
     * Root 是否已激活：su 节点存在即认为内核 root 已挂载；
     * 否则再尝试执行一次 `su -c id` 兜底（覆盖 su 不在常见路径的 Magisk 类环境）。
     */
    fun isRootActive(): Boolean {
        if (SU_PATHS.any { File(it).exists() }) return true
        val bin = SU_PATHS.firstOrNull { File(it).canExecute() } ?: return false
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf(bin, "-c", "id"))
            val finished = process.waitFor(2L, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            finished && process.exitValue() == 0
        }.getOrDefault(false)
    }

    /** 框架管理器是否已安装（Vector / LSPosed 任一），返回命中的包名。 */
    fun frameworkPackage(context: Context): String? = FRAMEWORK_PACKAGES.firstOrNull { pkg ->
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
    }

    /** 框架管理器的显示名，用于界面展示（如 Vector / LSPosed）。 */
    fun frameworkName(pkg: String?): String? = when (pkg) {
        VECTOR_PACKAGE -> "Vector"
        LSPOSED_PACKAGE -> "LSPosed"
        else -> null
    }

    /** 打开 GhostLock（root 晚加载激活器），返回是否成功拉起。 */
    fun openGhostLock(context: Context): Boolean {
        val pm = context.packageManager
        val attempts = mutableListOf<Intent>()
        // 优先标准桌面入口（ColorOS 对显式组件启动会弹「应用启动确认」）。
        pm.getLaunchIntentForPackage(GHOSTLOCK_PACKAGE)?.let(attempts::add)
        attempts += Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(GHOSTLOCK_PACKAGE, GHOSTLOCK_ACTIVITY)
        attempts.forEach { intent ->
            runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            }
        }
        return false
    }

    /**
     * 打开框架管理器（Vector / LSPosed）。管理器没有对外公开「直达某模块作用域页」的深链，
     * 这里尽力而为：优先标准启动入口，再试 Vector 的 LAUNCH_MANAGER 入口。
     * 返回是否成功拉起。
     */
    fun openFrameworkManager(context: Context): Boolean {
        val pm = context.packageManager
        val attempts = mutableListOf<Intent>()
        FRAMEWORK_PACKAGES.forEach { pkg -> pm.getLaunchIntentForPackage(pkg)?.let(attempts::add) }
        attempts += Intent(Intent.ACTION_MAIN)
            .addCategory("$VECTOR_PACKAGE.LAUNCH_MANAGER")
            .setPackage(VECTOR_PACKAGE)
        attempts.forEach { intent ->
            runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            }
        }
        return false
    }

    /**
     * 以 root 执行软重启（ksud soft-reboot）。用于「框架已安装但模块未注入」时尝试恢复；
     * 软重启会重启系统界面进程，本应用随之退出，属于预期行为。
     * 结果回调始终投递到主线程（回调里通常要弹 Toast）。
     */
    fun softReboot(onResult: (Boolean) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread({
            val bin = SU_PATHS.firstOrNull { File(it).canExecute() } ?: "/system/bin/su"
            val ok = runCatching {
                val process = Runtime.getRuntime().exec(arrayOf(bin, "-c", "$KSUD soft-reboot"))
                val finished = process.waitFor(5L, TimeUnit.SECONDS)
                if (!finished) process.destroyForcibly()
                finished && process.exitValue() == 0
            }.getOrDefault(false)
            mainHandler.post { onResult(ok) }
        }, "lingxi-soft-reboot").start()
    }
}
