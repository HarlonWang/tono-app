package com.tono.app.core.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

actual fun getAppVersion(): String =
    NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: UNKNOWN_APP_VERSION

actual fun isIosPlatform(): Boolean = true

actual fun getSystemLanguage(): String {
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
    return preferred.split("-").firstOrNull() ?: "en"
}

actual fun getDeviceInfo(): String {
    val device = UIDevice.currentDevice
    return "iOS ${device.systemVersion}; ${device.model}; channel=${ChannelHolder.get()}"
}

actual fun openInSystemBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = { _ -> })
}
