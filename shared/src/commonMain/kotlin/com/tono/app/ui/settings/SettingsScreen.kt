package com.tono.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tono.app.auth.AuthState
import com.tono.app.auth.LoginbaseAuthManager
import com.tono.app.auth.globalAuthManager
import com.tono.app.core.ProPaywall
import com.tono.app.core.billing.APP_STORE_MANAGE_URL
import com.tono.app.core.billing.AppStoreBilling
import com.tono.app.core.platform.openInSystemBrowser
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.remote.BillingRepository
import com.tono.app.resources.Res
import com.tono.app.resources.common_cancel
import com.tono.app.resources.common_ok
import com.tono.app.resources.delete_account_done
import com.tono.app.resources.settings_delete_account
import com.tono.app.resources.settings_account_signed_out
import com.tono.app.resources.settings_manage_subscription
import com.tono.app.resources.settings_pro_active
import com.tono.app.resources.settings_pro_upgrade
import com.tono.app.resources.settings_sign_in
import com.tono.app.resources.settings_sign_out
import com.tono.app.resources.settings_sign_out_confirm
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.tono.app.resources.settings_group_account
import com.tono.app.resources.settings_group_support
import com.tono.app.resources.settings_report
import wang.harlon.appsettings.ui.SettingsGroup
import wang.harlon.appsettings.GeneralSettingsGroup
import wang.harlon.appsettings.SettingsScreen as AppSettingsScreen
import wang.harlon.appsettings.ui.SettingsPage

const val PRIVACY_URL = "https://tonote.app/privacy"
const val TERMS_URL = "https://tonote.app/terms"

/** 举报入口（不良 AI 内容、滥用等），App Store 对 AI 生成内容类 App 要求可在 App 内找到；生成图的举报也走这里 */
const val REPORT_URL = "https://tonote.app/report"

/** 设置页：壳、滚动与通用行（外观 / 关于）来自 appsettings-kmp，这里摆账户组与举报入口。 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (SettingsPage) -> Unit) {
    val authState by globalAuthManager.authState.collectAsState()
    val email by globalSettingsManager.userEmail.collectAsState("")
    val isPro by globalSettingsManager.isPro.collectAsState(globalSettingsManager.currentIsPro())
    val scope = rememberCoroutineScope()
    val billing = remember { BillingRepository() }
    var confirmSignOut by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    var accountDeleted by remember { mutableStateOf(false) }
    val loginbase = globalAuthManager as? LoginbaseAuthManager
    val openPortal: () -> Unit = { scope.launch { billing.createPortal()?.overview?.let { openInSystemBrowser(it) } } }
    // iOS 只给 App Store 来源的订阅放管理入口（跳系统订阅页）；Paddle 门户属外部购买链接，iOS 上不放（3.1.1）
    val appStore = AppStoreBilling.available
    var proSource by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isPro) {
        if (appStore && isPro) proSource = billing.fetchSubscriptionSource()
    }
    val showManage = isPro && (!appStore || proSource == "apple")
    val manageSubscription: () -> Unit = if (appStore) ({ openInSystemBrowser(APP_STORE_MANAGE_URL) }) else openPortal
    val signedIn = authState is AuthState.LoggedIn

    AppSettingsScreen(onBack = onBack, onOpen = onOpen) {
        SettingsGroup(title = stringResource(Res.string.settings_group_account)) {
            if (signedIn) {
                settingsItem(
                    icon = Icons.Filled.AccountCircle,
                    title = { Text(email.ifBlank { stringResource(Res.string.settings_account_signed_out) }) },
                )
                settingsItem(
                    icon = if (isPro) Icons.Filled.WorkspacePremium else Icons.Filled.Star,
                    title = { Text(stringResource(if (isPro) Res.string.settings_pro_active else Res.string.settings_pro_upgrade)) },
                    onClick = if (isPro) null else ({ ProPaywall.open(ProPaywall.SOURCE_SETTINGS) }),
                )
                if (showManage) {
                    settingsItem(
                        icon = Icons.Filled.Lock,
                        title = { Text(stringResource(Res.string.settings_manage_subscription)) },
                        onClick = manageSubscription,
                    )
                }
                settingsItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = { Text(stringResource(Res.string.settings_sign_out)) },
                    onClick = { confirmSignOut = true },
                )
                if (loginbase != null) {
                    settingsItem(
                        icon = Icons.Filled.DeleteForever,
                        title = {
                            Text(stringResource(Res.string.settings_delete_account), color = MaterialTheme.colorScheme.error)
                        },
                        onClick = { deletingAccount = true },
                    )
                }
            } else {
                settingsItem(
                    icon = Icons.Filled.AccountCircle,
                    title = { Text(stringResource(Res.string.settings_sign_in)) },
                    onClick = { globalAuthManager.signIn(ProPaywall.SOURCE_SETTINGS) },
                )
                settingsItem(
                    icon = Icons.Filled.Star,
                    title = { Text(stringResource(Res.string.settings_pro_upgrade)) },
                    onClick = { ProPaywall.open(ProPaywall.SOURCE_SETTINGS) },
                )
            }
        }
        SettingsGroup(title = stringResource(Res.string.settings_group_support)) {
            settingsItem(
                icon = Icons.Filled.Flag,
                title = { Text(stringResource(Res.string.settings_report)) },
                onClick = { openInSystemBrowser(REPORT_URL) },
            )
        }
        GeneralSettingsGroup()
    }

    if (deletingAccount && loginbase != null) {
        DeleteAccountSheet(
            manager = loginbase,
            email = email,
            onManageSubscription = manageSubscription,
            onDeleted = {
                deletingAccount = false
                accountDeleted = true
            },
            onDismiss = { deletingAccount = false },
        )
    }

    if (accountDeleted) {
        AlertDialog(
            onDismissRequest = { accountDeleted = false },
            text = { Text(stringResource(Res.string.delete_account_done)) },
            confirmButton = {
                TextButton(onClick = { accountDeleted = false }) { Text(stringResource(Res.string.common_ok)) }
            },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            text = { Text(stringResource(Res.string.settings_sign_out_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    globalAuthManager.signOut()
                }) { Text(stringResource(Res.string.settings_sign_out), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
}
