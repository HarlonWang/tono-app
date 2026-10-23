package com.tono.app.core.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.ktx.awaitLogIn
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import kotlinx.coroutines.CancellationException

/** App Store 的订阅管理页；不是外部购买链接，iOS 上可放（3.1.1 只禁外部购买）。 */
const val APP_STORE_MANAGE_URL = "https://apps.apple.com/account/subscriptions"

/** Google Play 的订阅管理页：Android 上经 ACTION_VIEW 直接拉起 Play 商店的订阅页。 */
const val PLAY_STORE_MANAGE_URL = "https://play.google.com/store/account/subscriptions?package=com.tono.android"

/** 一档商店订阅：[plan] 取 "annual" / "monthly"，与 Paddle 档位同名；[price] 是商店（StoreKit / Play）的本地化价格。 */
data class StorePlan(val plan: String, val price: String, val amountMicros: Long)

sealed interface StorePurchaseResult {
    data object Success : StorePurchaseResult
    data object Cancelled : StorePurchaseResult
    data object Failed : StorePurchaseResult
}

/**
 * 本平台商店对应的 RevenueCat 公开 SDK key（本就随 App 分发）；null 表示本构建不走商店结算，
 * 订阅页回落 Paddle：Android 的 GitHub / F-Droid 渠道装不到 Play 结算。
 */
internal expect val storeApiKey: String?

/**
 * 商店订阅（App Store / Google Play，统一经 RevenueCat）。两端共用一套实现，平台只提供 [storeApiKey]。
 * 权益以服务端为准（webhook 落库后 /api/me 的 pro），这里只负责下单与恢复，不直接写本地 isPro。
 */
object StoreBilling {
    private const val ENTITLEMENT = "pro"

    val available: Boolean get() = storeApiKey != null

    private var userId: String? = null
    private var packages: Map<String, Package> = emptyMap()

    private fun purchases(): Purchases {
        val key = checkNotNull(storeApiKey) { "本构建不走商店结算" }
        // Android 侧 RevenueCat 经 AndroidX App Startup 自取 Context，不需要宿主传入
        if (!Purchases.isConfigured) Purchases.configure(PurchasesConfiguration(key))
        return Purchases.sharedInstance
    }

    /** RevenueCat 的 app_user_id 对齐 loginbase user_id，服务端 webhook 靠它归属订阅。 */
    fun identify(userId: String) {
        if (!available) return
        this.userId = userId
        val p = purchases()
        if (p.appUserID != userId) p.logIn(userId, onError = {}, onSuccess = { _, _ -> })
    }

    /** 退出登录 / 删号：回到匿名身份，避免下一个登录的人继承上一个人的购买归属。 */
    fun reset() {
        userId = null
        if (available && Purchases.isConfigured && !Purchases.sharedInstance.isAnonymous) {
            Purchases.sharedInstance.logOut(onError = {}, onSuccess = {})
        }
    }

    /** 当前 Offering 的年付、月付；拉不到返回 null。 */
    suspend fun loadPlans(): List<StorePlan>? = try {
        val offering = purchases().awaitOfferings().current
        val found = listOfNotNull(offering?.annual?.let { "annual" to it }, offering?.monthly?.let { "monthly" to it }).toMap()
        packages = found
        found.takeIf { it.size == 2 }?.map { (plan, pkg) ->
            StorePlan(plan, pkg.storeProduct.price.formatted, pkg.storeProduct.price.amountMicros)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun purchase(plan: String): StorePurchaseResult {
        val uid = userId ?: return StorePurchaseResult.Failed
        val pkg = packages[plan] ?: return StorePurchaseResult.Failed
        return try {
            // identify 的 logIn 是异步的，下单前确保身份已切到当前用户，否则购买归到匿名 ID
            val p = purchases()
            if (p.appUserID != uid) p.awaitLogIn(uid)
            p.awaitPurchase(pkg)
            StorePurchaseResult.Success
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) StorePurchaseResult.Cancelled else StorePurchaseResult.Failed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StorePurchaseResult.Failed
        }
    }

    /** true 恢复到 Pro，false 没有可恢复的订阅，null 恢复失败。 */
    suspend fun restore(): Boolean? = try {
        purchases().awaitRestore().entitlements.active.containsKey(ENTITLEMENT)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /**
     * 生效中 Pro 来源（/api/billing/subscription 的 source）对应的订阅管理页：商店来源跳各自系统页，
     * 跨平台也成立（iOS 买的在 Android 上照样跳 App Store）。Paddle 来源返回 null：商店构建里 Paddle 门户属外部购买链接，不放。
     */
    fun manageUrlOf(source: String?): String? = when (source) {
        "apple" -> APP_STORE_MANAGE_URL
        "google" -> PLAY_STORE_MANAGE_URL
        else -> null
    }
}
