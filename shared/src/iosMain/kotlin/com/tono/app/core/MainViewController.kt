package com.tono.app.core

import androidx.compose.ui.window.ComposeUIViewController
import com.tono.app.auth.initLoginbaseAuth
import com.tono.app.chat.installTonoChatHost
import com.tono.app.core.analytics.analyticsConfig
import com.tono.app.core.platform.ChannelHolder
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.UIKit.UIViewController
import wang.harlon.eventbase.Eventbase
import wang.harlon.eventbase.init

@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController {
    ChannelHolder.set("appstore")
    // iOS 没有 Application 那一层，埋点在这里初始化；init 先到先得，重复调用无副作用
    Eventbase.init(analyticsConfig(isDebug = Platform.isDebugBinary))
    installTonoChatHost()
    initLoginbaseAuth()
    return ComposeUIViewController { App() }
}
