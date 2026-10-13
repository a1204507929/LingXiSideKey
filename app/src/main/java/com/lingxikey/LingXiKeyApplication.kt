package com.lingxikey.custom

import android.app.Application
import com.lingxikey.custom.data.SettingsStore

class LingXiKeyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.initializeRemoteStorage(this)
    }
}
