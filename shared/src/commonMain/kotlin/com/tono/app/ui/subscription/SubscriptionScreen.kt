package com.tono.app.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tono.app.auth.AuthState
import com.tono.app.auth.globalAuthManager
import com.tono.app.core.ProCheckout
import com.tono.app.core.billing.StoreBilling
import com.tono.app.core.billing.StorePlan
import com.tono.app.core.billing.StorePurchaseResult
import com.tono.app.core.platform.isIosPlatform
import com.tono.app.core.platform.openInSystemBrowser
import com.tono.app.core.platform.requestLanguage
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.model.PricesResponse
import com.tono.app.data.remote.AppConfig
import com.tono.app.data.remote.BillingRepository
import com.tono.app.data.remote.UserRepository
import com.tono.app.resources.Res
import com.tono.app.resources.common_back
import com.tono.app.resources.paywall_already_pro
import com.tono.app.resources.paywall_benefit_images
import com.tono.app.resources.paywall_benefit_models
import com.tono.app.resources.paywall_benefit_quota
import com.tono.app.resources.paywall_benefit_search
import com.tono.app.resources.paywall_benefit_voice
import com.tono.app.resources.paywall_checkout_failed
import com.tono.app.resources.paywall_ios_disclosure
import com.tono.app.resources.paywall_ios_pro_via_web
import com.tono.app.resources.paywall_ios_refund_note
import com.tono.app.resources.paywall_play_disclosure
import com.tono.app.resources.paywall_play_pro_via_web
import com.tono.app.resources.paywall_play_refund_note
import com.tono.app.resources.paywall_store_privacy
import com.tono.app.resources.paywall_store_purchase_failed
import com.tono.app.resources.paywall_store_restore
import com.tono.app.resources.paywall_store_restore_done
import com.tono.app.resources.paywall_store_restore_none
import com.tono.app.resources.paywall_store_terms
import com.tono.app.resources.settings_manage_subscription
import com.tono.app.resources.paywall_cta_sign_in
import com.tono.app.resources.paywall_cta_subscribe
import com.tono.app.resources.paywall_plan_annual
import com.tono.app.resources.paywall_plan_monthly
import com.tono.app.resources.paywall_price_unavailable
import com.tono.app.resources.paywall_refund_note
import com.tono.app.resources.paywall_savings
import com.tono.app.resources.paywall_subtitle
import com.tono.app.resources.paywall_title
import com.tono.app.ui.settings.PRIVACY_URL
import com.tono.app.ui.settings.TERMS_URL
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val SOURCE_PAYWALL = "paywall"

/**
 * 订阅页。文案以 /api/app-config 的 pro_paywall 为准，本地 strings 只是从未拉到配置时的兜底。
 * 商店构建（iOS、Android Play 渠道）：只走商店结算（[StoreBilling]），价格取商店本地化价，文案换成
 * pro_paywall.app_store / play_store，不出现任何 Paddle 价格或链接（App Store 3.1.1、Play 付款政策第 4 条）。
 * 其他构建（GitHub / F-Droid）：价格来自 /api/billing/prices，下单经 /api/billing/checkout 拿 Paddle 收银台链接后外跳浏览器。
 * Pro 一律以服务端 webhook 落库后的 /api/me 为准。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val lang = remember { requestLanguage() }
    val remote by AppConfig.current.collectAsState()
    val paywall = remote?.proPaywall
    val isPro by globalSettingsManager.isPro.collectAsState(globalSettingsManager.currentIsPro())
    val authState by globalAuthManager.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val billing = remember { BillingRepository() }
    val userRepository = remember { UserRepository() }
    val storeBilling = StoreBilling.available
    val ios = remember { isIosPlatform() }
    val store = if (ios) paywall?.appStore else paywall?.playStore

    var prices by remember { mutableStateOf<PricesResponse?>(null) }
    var storePlans by remember { mutableStateOf<List<StorePlan>?>(null) }
    var pricesLoaded by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf("annual") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var proSource by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (storeBilling) storePlans = StoreBilling.loadPlans() else prices = billing.fetchPrices()
        pricesLoaded = true
    }
    LaunchedEffect(isPro) {
        if (isPro) proSource = billing.fetchSubscriptionSource()
    }

    val checkoutFailed = paywall?.checkoutFailed?.forLang(lang) ?: stringResource(Res.string.paywall_checkout_failed)
    val purchaseFailed = store?.purchaseFailed?.forLang(lang) ?: stringResource(Res.string.paywall_store_purchase_failed)
    val restoreNone = store?.restoreNone?.forLang(lang) ?: stringResource(Res.string.paywall_store_restore_none)
    val restoreDone = store?.restoreDone?.forLang(lang) ?: stringResource(Res.string.paywall_store_restore_done)

    val options: List<PlanOption>? = if (storeBilling) {
        storePlans?.let { plans ->
            val annual = plans.first { it.plan == "annual" }
            val monthly = plans.first { it.plan == "monthly" }
            val savings = (100 - annual.amountMicros * 100 / (monthly.amountMicros * 12)).toInt().takeIf { it > 0 }
            listOf(PlanOption("annual", annual.price, savings), PlanOption("monthly", monthly.price, null))
        }
    } else {
        prices?.takeIf { it.available }?.let {
            listOf(PlanOption("annual", it.annual!!.formatted, it.savingsPercent), PlanOption("monthly", it.monthly!!.formatted, null))
        }
    }

    /** 付款成功后等 webhook 落库：轮询 /api/me，超时的交给 [ProReconcileHost] 回前台再对账。 */
    suspend fun awaitPro() {
        val attempt = ProCheckout.reconcile { userRepository.syncMe() != null && globalSettingsManager.currentIsPro() }
        if (attempt != null) ProCheckout.markActivated(attempt)
    }

    /** 购买与恢复都要先登录：RevenueCat 的 app_user_id 必须是 loginbase user_id，服务端才认得这笔订阅。 */
    fun requireSignIn(): Boolean {
        if (authState is AuthState.LoggedIn) return true
        globalAuthManager.signIn(SOURCE_PAYWALL)
        return false
    }

    fun subscribe() {
        if (!requireSignIn()) return
        error = null
        notice = null
        busy = true
        scope.launch {
            if (storeBilling) {
                when (StoreBilling.purchase(plan)) {
                    StorePurchaseResult.Success -> {
                        ProCheckout.markOpened(plan)
                        awaitPro()
                    }
                    StorePurchaseResult.Cancelled -> Unit
                    StorePurchaseResult.Failed -> error = purchaseFailed
                }
            } else {
                val checkout = billing.createCheckout(plan)
                if (checkout == null) {
                    error = checkoutFailed
                } else {
                    ProCheckout.markOpened(plan)
                    openInSystemBrowser(checkout.url)
                }
            }
            busy = false
        }
    }

    fun restore() {
        if (!requireSignIn()) return
        error = null
        notice = null
        busy = true
        scope.launch {
            when (StoreBilling.restore()) {
                true -> {
                    awaitPro()
                    notice = restoreDone
                }
                false -> notice = restoreNone
                null -> error = purchaseFailed
            }
            busy = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = paywall?.title?.forLang(lang) ?: stringResource(Res.string.paywall_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = paywall?.subtitle?.forLang(lang) ?: stringResource(Res.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val benefits = paywall?.benefits?.mapNotNull { it.text?.forLang(lang) }?.takeIf { it.isNotEmpty() }
                ?: listOf(
                    stringResource(Res.string.paywall_benefit_quota),
                    stringResource(Res.string.paywall_benefit_models),
                    stringResource(Res.string.paywall_benefit_voice),
                    stringResource(Res.string.paywall_benefit_search),
                    stringResource(Res.string.paywall_benefit_images),
                )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                benefits.forEach { benefit ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(benefit, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (isPro) {
                Text(
                    text = paywall?.alreadyPro?.forLang(lang) ?: stringResource(Res.string.paywall_already_pro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                // 商店来源可跳各自系统的订阅管理；Paddle 来源在商店构建里只告知，不放任何外链
                val manageUrl = StoreBilling.manageUrlOf(proSource)
                if (manageUrl != null) {
                    OutlinedButton(onClick = { openInSystemBrowser(manageUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Text(store?.manage?.forLang(lang) ?: stringResource(Res.string.settings_manage_subscription))
                    }
                } else if (storeBilling && proSource == "paddle") {
                    Text(
                        text = store?.proViaWeb?.forLang(lang)
                            ?: stringResource(if (ios) Res.string.paywall_ios_pro_via_web else Res.string.paywall_play_pro_via_web),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            when {
                !pricesLoaded -> LoadingIndicator(modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally))
                options == null -> Text(
                    stringResource(Res.string.paywall_price_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> options.forEach { option ->
                    PlanCard(
                        selected = plan == option.plan,
                        label = stringResource(if (option.plan == "annual") Res.string.paywall_plan_annual else Res.string.paywall_plan_monthly),
                        price = option.price,
                        note = option.savingsPercent?.let { stringResource(Res.string.paywall_savings).replace("%d", it.toString()) },
                        onClick = { plan = option.plan },
                    )
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            Button(
                onClick = { subscribe() },
                enabled = !busy && options != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    val signedIn = authState is AuthState.LoggedIn
                    Text(
                        if (signedIn) paywall?.cta?.subscribe?.forLang(lang) ?: stringResource(Res.string.paywall_cta_subscribe)
                        else paywall?.cta?.signIn?.forLang(lang) ?: stringResource(Res.string.paywall_cta_sign_in),
                    )
                }
            }
            if (storeBilling) {
                TextButton(onClick = { restore() }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(store?.restore?.forLang(lang) ?: stringResource(Res.string.paywall_store_restore))
                }
                StoreFooter(
                    refundNote = store?.refundNote?.forLang(lang)
                        ?: stringResource(if (ios) Res.string.paywall_ios_refund_note else Res.string.paywall_play_refund_note),
                    disclosure = store?.disclosure?.forLang(lang)
                        ?: stringResource(if (ios) Res.string.paywall_ios_disclosure else Res.string.paywall_play_disclosure),
                )
            } else {
                Text(
                    text = paywall?.refundNote?.forLang(lang) ?: stringResource(Res.string.paywall_refund_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class PlanOption(val plan: String, val price: String, val savingsPercent: Int?)

/** App Store 3.1.2 要求（Play 同样照做）：自动续费说明 + 隐私政策与使用条款（EULA）链接，紧贴购买按钮下方。 */
@Composable
private fun StoreFooter(refundNote: String, disclosure: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(refundNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(disclosure, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { openInSystemBrowser(PRIVACY_URL) }) { Text(stringResource(Res.string.paywall_store_privacy)) }
            TextButton(onClick = { openInSystemBrowser(TERMS_URL) }) { Text(stringResource(Res.string.paywall_store_terms)) }
        }
    }
}

@Composable
private fun PlanCard(selected: Boolean, label: String, price: String, note: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(price, style = MaterialTheme.typography.titleMedium)
        }
    }
}
