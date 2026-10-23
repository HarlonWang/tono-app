package com.tono.app.core.billing

import com.tono.app.core.platform.ChannelHolder

/**
 * RevenueCat 的 Google Play 公开 SDK key（goog_ 开头，本就随 APK 分发）。
 * 空串表示 RevenueCat 还没配 Play App，此时与非 Play 渠道一样回落 Paddle。
 */
private const val PLAY_API_KEY = ""

/** 只有 Play 渠道走 Play 结算：GitHub / F-Droid 包装不到 Play 结算，继续外跳 Paddle。 */
internal actual val storeApiKey: String?
    get() = PLAY_API_KEY.takeIf { it.isNotEmpty() && ChannelHolder.get() == "play" }
