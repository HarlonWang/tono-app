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

actual object AppStoreBilling {
    /** 公开 SDK key，本就随 App 分发 */
    private const val API_KEY = "appl_ZhxzOtkxjGbhauMQgmsMjmoRooP"
    private const val ENTITLEMENT = "pro"

    actual val available: Boolean = true

    private var userId: String? = null
    private var packages: Map<String, Package> = emptyMap()

    private fun purchases(): Purchases {
        if (!Purchases.isConfigured) Purchases.configure(PurchasesConfiguration(API_KEY))
        return Purchases.sharedInstance
    }

    actual fun identify(userId: String) {
        this.userId = userId
        val p = purchases()
        if (p.appUserID != userId) p.logIn(userId, onError = {}, onSuccess = { _, _ -> })
    }

    actual fun reset() {
        userId = null
        if (Purchases.isConfigured && !Purchases.sharedInstance.isAnonymous) {
            Purchases.sharedInstance.logOut(onError = {}, onSuccess = {})
        }
    }

    actual suspend fun loadPlans(): List<AppStorePlan>? = try {
        val offering = purchases().awaitOfferings().current
        val found = listOfNotNull(offering?.annual?.let { "annual" to it }, offering?.monthly?.let { "monthly" to it }).toMap()
        packages = found
        found.takeIf { it.size == 2 }?.map { (plan, pkg) ->
            AppStorePlan(plan, pkg.storeProduct.price.formatted, pkg.storeProduct.price.amountMicros)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    actual suspend fun purchase(plan: String): AppStorePurchaseResult {
        val uid = userId ?: return AppStorePurchaseResult.Failed
        val pkg = packages[plan] ?: return AppStorePurchaseResult.Failed
        return try {
            // identify 的 logIn 是异步的，下单前确保身份已切到当前用户，否则购买归到匿名 ID
            val p = purchases()
            if (p.appUserID != uid) p.awaitLogIn(uid)
            p.awaitPurchase(pkg)
            AppStorePurchaseResult.Success
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) AppStorePurchaseResult.Cancelled else AppStorePurchaseResult.Failed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppStorePurchaseResult.Failed
        }
    }

    actual suspend fun restore(): Boolean? = try {
        purchases().awaitRestore().entitlements.active.containsKey(ENTITLEMENT)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
