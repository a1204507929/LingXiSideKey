package com.lingxikey.custom.xposed

import android.os.Handler
import android.os.Looper

internal object HandlerBridge {
    private val handler = Handler(Looper.getMainLooper())
    fun post(block: () -> Unit) {
        handler.post(block)
    }
}
