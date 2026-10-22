package com.tono.app.core.billing

actual object AppStoreBilling {
    actual val available: Boolean = false
    actual fun identify(userId: String) {}
    actual fun reset() {}
    actual suspend fun loadPlans(): List<AppStorePlan>? = null
    actual suspend fun purchase(plan: String): AppStorePurchaseResult = AppStorePurchaseResult.Failed
    actual suspend fun restore(): Boolean? = null
}
