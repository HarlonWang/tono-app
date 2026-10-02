package com.tono.app.auth

import io.ktor.client.engine.darwin.Darwin
import wang.harlon.loginbase.NSUserDefaultsTokenStore

/**
 * 显式给 engine：Kotlin/Native 上缺省发现拿不到时要到第一个请求才炸。
 * TokenStore 是 NSUserDefaults 而非 Keychain，卸载即丢会话。
 */
fun initLoginbaseAuth(): LoginbaseAuthManager =
    initLoginbaseAuth(NSUserDefaultsTokenStore()) { Darwin.create() }
