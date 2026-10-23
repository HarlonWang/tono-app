package com.tono.app.core.billing

import com.tono.app.core.platform.ChannelHolder

/** RevenueCat 的 Google Play 公开 SDK key（本就随 APK 分发）；置空可临时回落 Paddle。 */
private const val PLAY_API_KEY = "goog_vhWijZpqjsXjViKBULReAeOwTFh"

/** 只有 Play 渠道走 Play 结算：GitHub / F-Droid 包装不到 Play 结算，继续外跳 Paddle。 */
internal actual val storeApiKey: String?
    get() = PLAY_API_KEY.takeIf { it.isNotEmpty() && ChannelHolder.get() == "play" }
