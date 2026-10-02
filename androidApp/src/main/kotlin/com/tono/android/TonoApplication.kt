package com.tono.android

import android.app.Application
import com.tono.app.chat.installTonoChatHost
import com.tono.app.core.platform.AndroidContextHolder
import com.tono.app.core.platform.ChannelHolder

class TonoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ChannelHolder.set(BuildConfig.CHANNEL)
        AndroidContextHolder.initialize(this)
        installTonoChatHost()
    }
}
