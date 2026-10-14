package com.tono.android

import android.app.Application
import com.tono.app.chat.installTonoChatHost
import com.tono.app.core.analytics.analyticsConfig
import com.tono.app.core.platform.AndroidContextHolder
import com.tono.app.core.platform.ChannelHolder
import wang.harlon.eventbase.Eventbase
import wang.harlon.eventbase.init

class TonoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ChannelHolder.set(BuildConfig.CHANNEL)
        AndroidContextHolder.initialize(this)
        // 在 ChannelHolder.set 与 AndroidContextHolder 之后：渠道与版本号都从那里读
        Eventbase.init(context = this, config = analyticsConfig(isDebug = BuildConfig.DEBUG))
        installTonoChatHost()
    }
}
