package com.lingxikey.custom.xposed

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import com.lingxikey.custom.RootCommandTrampolineActivity
import com.lingxikey.custom.RootCommandReceiver
import com.lingxikey.custom.RootCommandService
import com.lingxikey.custom.onXposedFailure

/** Runs in the process that handled the side key; never blocks its input thread. */
internal object ShellCommandRunner {
    fun executeAsRoot(context: Context, command: String, showToast: Boolean = true): Boolean {
        return executeViaActivity(context, command, showToast)
    }

    fun executeViaReceiver(context: Context, command: String, showToast: Boolean = true): Boolean {
        return runCatching {
            context.sendBroadcast(appIntent(RootCommandReceiver::class.java, RootCommandReceiver.ACTION, command, showToast))
            XposedBridge.log("LingXiKeyCustom: dispatched root command to app receiver")
            true
        }.onXposedFailure("receiver dispatch").getOrDefault(false)
    }

    fun executeViaService(context: Context, command: String, showToast: Boolean = true): Boolean {
        return runCatching {
            val intent = appIntent(RootCommandService::class.java, RootCommandService.ACTION, command, showToast)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            XposedBridge.log("LingXiKeyCustom: dispatched root command to app service")
            true
        }.onXposedFailure("service dispatch").getOrDefault(false)
    }

    fun executeViaActivity(context: Context, command: String, showToast: Boolean = true): Boolean {
        return runCatching {
            val intent = appIntent(
                RootCommandTrampolineActivity::class.java,
                RootCommandTrampolineActivity.ACTION,
                command,
                showToast
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(intent)
            XposedBridge.log("LingXiKeyCustom: dispatched root command to app trampoline")
            true
        }.onXposedFailure("activity dispatch").getOrDefault(false)
    }

    private fun appIntent(component: Class<*>, action: String, command: String, showToast: Boolean): Intent = Intent()
        .setComponent(ComponentName(PACKAGE, component.name))
        .setAction(action)
        .putExtra("command", command)
        .putExtra("show_toast", showToast)

    private const val PACKAGE = "com.lingxikey.custom"
}
