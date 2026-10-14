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
import com.tono.app.core.platform.openInSystemBrowser
import com.tono.app.core.platform.requestLanguage
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.model.PricesResponse
import com.tono.app.data.remote.AppConfig
import com.tono.app.data.remote.BillingRepository
import com.tono.app.resources.Res
import com.tono.app.resources.common_back
import com.tono.app.resources.paywall_already_pro
import com.tono.app.resources.paywall_benefit_images
import com.tono.app.resources.paywall_benefit_models
import com.tono.app.resources.paywall_benefit_quota
import com.tono.app.resources.paywall_benefit_search
import com.tono.app.resources.paywall_benefit_voice
import com.tono.app.resources.paywall_checkout_failed
import com.tono.app.resources.paywall_cta_sign_in
import com.tono.app.resources.paywall_cta_subscribe
import com.tono.app.resources.paywall_plan_annual
import com.tono.app.resources.paywall_plan_monthly
import com.tono.app.resources.paywall_price_unavailable
import com.tono.app.resources.paywall_refund_note
import com.tono.app.resources.paywall_savings
import com.tono.app.resources.paywall_subtitle
import com.tono.app.resources.paywall_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val SOURCE_PAYWALL = "paywall"

/**
 * 订阅页。文案以 /api/app-config 的 pro_paywall 为准，本地 strings 只是从未拉到配置时的兜底；
 * 价格来自 /api/billing/prices，下单经 /api/billing/checkout 拿 Paddle 收银台链接后外跳浏览器。
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

    var prices by remember { mutableStateOf<PricesResponse?>(null) }
    var pricesLoaded by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf("annual") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        prices = billing.fetchPrices()
        pricesLoaded = true
    }

    val checkoutFailed = paywall?.checkoutFailed?.forLang(lang) ?: stringResource(Res.string.paywall_checkout_failed)

    fun subscribe() {
        if (authState !is AuthState.LoggedIn) {
            globalAuthManager.signIn(SOURCE_PAYWALL)
            return
        }
        error = null
        busy = true
        scope.launch {
            val checkout = billing.createCheckout(plan)
            if (checkout == null) {
                error = checkoutFailed
            } else {
                ProCheckout.markOpened(plan)
                openInSystemBrowser(checkout.url)
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
                return@Column
            }

            when {
                !pricesLoaded -> LoadingIndicator(modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally))
                prices?.available != true -> Text(
                    stringResource(Res.string.paywall_price_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val p = prices!!
                    PlanCard(
                        selected = plan == "annual",
                        label = stringResource(Res.string.paywall_plan_annual),
                        price = p.annual!!.formatted,
                        note = p.savingsPercent?.let { stringResource(Res.string.paywall_savings).replace("%d", it.toString()) },
                        onClick = { plan = "annual" },
                    )
                    PlanCard(
                        selected = plan == "monthly",
                        label = stringResource(Res.string.paywall_plan_monthly),
                        price = p.monthly!!.formatted,
                        note = null,
                        onClick = { plan = "monthly" },
                    )
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = { subscribe() },
                enabled = !busy && prices?.available == true,
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
            Text(
                text = paywall?.refundNote?.forLang(lang) ?: stringResource(Res.string.paywall_refund_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
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
