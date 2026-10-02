package com.tono.app.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import java.lang.ref.WeakReference

object AndroidContextHolder {
    private var contextRef: WeakReference<Context>? = null

    fun initialize(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }

    fun get(): Context? = contextRef?.get()
}

actual fun getAppVersion(): String {
    val context = AndroidContextHolder.get() ?: return UNKNOWN_APP_VERSION
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: UNKNOWN_APP_VERSION
    } catch (e: Exception) {
        Log.w("Platform", "getAppVersion failed", e)
        UNKNOWN_APP_VERSION
    }
}

actual fun isIosPlatform(): Boolean = false

actual fun getSystemLanguage(): String = java.util.Locale.getDefault().language

actual fun getDeviceInfo(): String =
    "Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; channel=${ChannelHolder.get()}"

actual fun openInSystemBrowser(url: String) {
    val context = AndroidContextHolder.get() ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("Platform", "openInSystemBrowser failed", e)
    }
}
