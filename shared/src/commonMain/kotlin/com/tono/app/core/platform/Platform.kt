package com.tono.app.core.platform

const val UNKNOWN_APP_VERSION: String = "unknown"

expect fun getAppVersion(): String

expect fun isIosPlatform(): Boolean

/** 系统语言的 ISO 639-1 码（"zh" / "en" / …）。 */
expect fun getSystemLanguage(): String

/** 系统首选语言的完整 BCP-47 标签（"zh-Hans-CN"），埋点用。 */
expect fun getSystemLocaleTag(): String

expect fun getDeviceInfo(): String

/** 系统浏览器打开外链。Paddle 收银台必须应用外打开：三方支付跳转在内置 WebView 跑不通。 */
expect fun openInSystemBrowser(url: String)

fun getUserAgent(): String = "Tono/${getAppVersion()} (${getDeviceInfo()})"

/** 请求语言：服务端只认 zh / en 两档。 */
fun requestLanguage(): String = if (getSystemLanguage() == "zh") "zh" else "en"

object ChannelHolder {
    private var channel: String = "unknown"
    fun set(value: String) { channel = value }
    fun get(): String = channel
}
