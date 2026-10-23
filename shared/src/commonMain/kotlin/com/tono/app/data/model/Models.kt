package com.tono.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(val user: MeUser, val pro: Boolean = false)

@Serializable
data class MeUser(
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CheckoutResponse(
    val url: String,
    @SerialName("transaction_id") val transactionId: String? = null,
)

@Serializable
data class PricesResponse(
    val currency: String? = null,
    val country: String? = null,
    val annual: PriceView? = null,
    val monthly: PriceView? = null,
    @SerialName("annual_savings_percent") val savingsPercent: Int? = null,
) {
    val available: Boolean get() = annual != null && monthly != null
}

@Serializable
data class PriceView(val formatted: String)

@Serializable
data class PortalResponse(
    val overview: String? = null,
    val cancel: String? = null,
    @SerialName("update_payment") val updatePayment: String? = null,
)

/** /api/billing/subscription 只取来源；Paddle 行与 Apple 行的细节客户端不用。 */
@Serializable
data class SubscriptionResponse(val source: String? = null)

@Serializable
data class AppConfigResponse(
    @SerialName("min_version") val minVersion: String? = null,
    @SerialName("chat_images") val chatImages: ChatImagesRemoteConfig? = null,
    @SerialName("chat_voice") val chatVoice: ChatVoiceRemoteConfig? = null,
    @SerialName("pro_paywall") val proPaywall: ProPaywallRemoteConfig? = null,
    @SerialName("quota_help") val quotaHelp: QuotaHelpRemoteConfig? = null,
)

@Serializable
data class ChatImagesRemoteConfig(
    @SerialName("max_count") val maxCount: Int? = null,
    @SerialName("per_image_jpeg_kb") val perImageJpegKb: Int? = null,
)

@Serializable
data class ChatVoiceRemoteConfig(@SerialName("max_duration_ms") val maxDurationMs: Int? = null)

@Serializable
data class ProPaywallRemoteConfig(
    val title: LocalizedText? = null,
    val subtitle: LocalizedText? = null,
    val benefits: List<ProBenefitRow> = emptyList(),
    val cta: PaywallCtaRemoteConfig? = null,
    @SerialName("refund_note") val refundNote: LocalizedText? = null,
    @SerialName("already_pro") val alreadyPro: LocalizedText? = null,
    @SerialName("checkout_failed") val checkoutFailed: LocalizedText? = null,
    /** 商店结算专用替换字段：iOS 读 app_store（App Store 审核口径），Android Play 渠道读 play_store */
    @SerialName("app_store") val appStore: StorePaywallRemoteConfig? = null,
    @SerialName("play_store") val playStore: StorePaywallRemoteConfig? = null,
)

@Serializable
data class StorePaywallRemoteConfig(
    @SerialName("refund_note") val refundNote: LocalizedText? = null,
    val disclosure: LocalizedText? = null,
    val restore: LocalizedText? = null,
    @SerialName("restore_none") val restoreNone: LocalizedText? = null,
    @SerialName("restore_done") val restoreDone: LocalizedText? = null,
    @SerialName("purchase_failed") val purchaseFailed: LocalizedText? = null,
    @SerialName("pro_via_web") val proViaWeb: LocalizedText? = null,
    val manage: LocalizedText? = null,
)

@Serializable
data class PaywallCtaRemoteConfig(
    val subscribe: LocalizedText? = null,
    @SerialName("sign_in") val signIn: LocalizedText? = null,
    @SerialName("view_price") val viewPrice: LocalizedText? = null,
)

@Serializable
data class ProBenefitRow(val icon: String? = null, val text: LocalizedText? = null)

@Serializable
data class QuotaHelpRemoteConfig(
    val title: LocalizedText? = null,
    val paragraphs: List<LocalizedText> = emptyList(),
)

@Serializable
data class LocalizedText(val zh: String? = null, val en: String? = null) {
    fun forLang(lang: String): String? = if (lang == "zh") zh ?: en else en
}
