package com.tono.app.core.analytics

import com.tono.app.core.platform.ChannelHolder
import com.tono.app.core.platform.getAppVersion
import com.tono.app.core.platform.getSystemLocaleTag
import com.tono.app.core.platform.isIosPlatform
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.remote.API_HOST
import wang.harlon.eventbase.Eventbase
import wang.harlon.eventbase.EventbaseConfig

/** 公开 key，只用于路由与开关，进 APK 无妨；与 tono-api wrangler.toml 的 EVENTBASE_APP_KEY 一致。 */
private const val APP_KEY = "tono-c8377572"

/**
 * [Eventbase.init] 的入参。**必须在 ChannelHolder.set 之后调**，否则渠道恒为 unknown。
 *
 * `installId` 传 App 自己那个：chat 的 X-Install-Id 与 checkout 的 custom_data 用的都是它，
 * 服务端按它补发配额拦截与成单事件，两端各生成一个就串不成漏斗。
 */
fun analyticsConfig(isDebug: Boolean): EventbaseConfig = EventbaseConfig(
    endpoint = "$API_HOST/t",
    appKey = APP_KEY,
    appVersion = getAppVersion(),
    platform = if (isIosPlatform()) "ios" else "android",
    channel = ChannelHolder.get(),
    locale = getSystemLocaleTag(),
    isDebug = isDebug,
    logEvents = isDebug,
    installId = globalSettingsManager.getOrCreateInstallId(),
)

/** 全 App 唯一的上报出口。[flow] 用于跨进程漏斗，见 [Eventbase.startFlow]。 */
fun track(event: AppEvent, flow: String? = null) {
    Eventbase.track(event, flow)
}

/** 登录后关联账号（loginbase 的 user id，不是邮箱）；登出传 null。 */
fun setAnalyticsUser(userId: String?) {
    Eventbase.setUserId(userId)
}
