package com.lingxikey.custom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class RootCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val command = intent.getStringExtra(EXTRA_COMMAND)?.takeIf { it.isNotBlank() } ?: return
        if (intent.getBooleanExtra(EXTRA_SHOW_TOAST, true)) {
            Toast.makeText(context.applicationContext, "已收到 Shell 广播", Toast.LENGTH_SHORT).show()
        }
        val pending = goAsync()
        Thread {
            try {
                val process = execRoot(command)
                val finished = process.waitFor(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                if (!finished) process.destroyForcibly()
                if (!finished || process.exitValue() != 0) {
                    Log.e(TAG, "root command failed or timed out")
                }
            } catch (error: Throwable) {
                Log.e(TAG, "root command failed", error)
            } finally {
                pending.finish()
            }
        }.apply {
            isDaemon = true
            name = "CustomSideButtonRootCommand"
            start()
        }
    }

    /** Runtime.exec 不解析 PATH，必须用 su 的绝对路径（见 RootCommandService.execRoot）。 */
    private fun execRoot(command: String): Process {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/debug_ramdisk/su",
            "/data/adb/ksu/bin/su"
        )
        val available = suPaths.filter { java.io.File(it).canExecute() }
        val candidates = available.ifEmpty { listOf("su") }
        var lastError: Throwable? = null
        for (bin in candidates) {
            try {
                return Runtime.getRuntime().exec(arrayOf(bin, "-c", command))
            } catch (error: Throwable) {
                lastError = error
                Log.w(TAG, "exec failed with $bin, trying next", error)
            }
        }
        throw lastError ?: IllegalStateException("no usable su binary")
    }

    companion object {
        const val ACTION = "com.lingxikey.custom.EXECUTE_ROOT"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_SHOW_TOAST = "show_toast"
        private const val TAG = "LingXiKeyCustom"
        private const val TIMEOUT_SECONDS = 10L
    }
}
