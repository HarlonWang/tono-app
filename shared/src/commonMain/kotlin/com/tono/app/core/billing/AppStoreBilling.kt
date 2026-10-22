package com.tono.app.core.billing

/** App Store 的订阅管理页；不是外部购买链接，iOS 上可放（3.1.1 只禁外部购买）。 */
const val APP_STORE_MANAGE_URL = "https://apps.apple.com/account/subscriptions"

/** 一档 App Store 订阅：[plan] 取 "annual" / "monthly"，与 Paddle 档位同名；[price] 是 StoreKit 本地化价格。 */
data class AppStorePlan(val plan: String, val price: String, val amountMicros: Long)

sealed interface AppStorePurchaseResult {
    data object Success : AppStorePurchaseResult
    data object Cancelled : AppStorePurchaseResult
    data object Failed : AppStorePurchaseResult
}

/**
 * App Store 订阅（经 RevenueCat）。只有 iOS 有实现，Android 走 Paddle、[available] 恒为 false。
 * 权益以服务端为准（webhook 落库后 /api/me 的 pro），这里只负责下单与恢复，不直接写本地 isPro。
 */
expect object AppStoreBilling {
    val available: Boolean

    /** RevenueCat 的 app_user_id 对齐 loginbase user_id，服务端 webhook 靠它归属订阅。 */
    fun identify(userId: String)

    /** 退出登录 / 删号：回到匿名身份，避免下一个登录的人继承上一个人的购买归属。 */
    fun reset()

    /** 当前 Offering 的年付、月付；拉不到返回 null。 */
    suspend fun loadPlans(): List<AppStorePlan>?

    suspend fun purchase(plan: String): AppStorePurchaseResult

    /** true 恢复到 Pro，false 没有可恢复的订阅，null 恢复失败。 */
    suspend fun restore(): Boolean?
}
