package com.tono.app.core

import androidx.compose.ui.window.ComposeUIViewController
import com.tono.app.auth.initLoginbaseAuth
import com.tono.app.chat.installTonoChatHost
import com.tono.app.core.platform.ChannelHolder
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    ChannelHolder.set("appstore")
    installTonoChatHost()
    initLoginbaseAuth()
    return ComposeUIViewController { App() }
}
