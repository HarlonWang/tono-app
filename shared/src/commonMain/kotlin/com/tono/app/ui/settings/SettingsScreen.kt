package com.tono.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tono.app.auth.AuthState
import com.tono.app.auth.globalAuthManager
import com.tono.app.core.ProPaywall
import com.tono.app.core.platform.openInSystemBrowser
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.remote.BillingRepository
import com.tono.app.resources.Res
import com.tono.app.resources.common_back
import com.tono.app.resources.common_cancel
import com.tono.app.resources.settings_account_signed_out
import com.tono.app.resources.settings_manage_subscription
import com.tono.app.resources.settings_pro_active
import com.tono.app.resources.settings_pro_upgrade
import com.tono.app.resources.settings_sign_in
import com.tono.app.resources.settings_sign_out
import com.tono.app.resources.settings_sign_out_confirm
import com.tono.app.resources.settings_about
import com.tono.app.resources.settings_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToAbout: () -> Unit) {
    val authState by globalAuthManager.authState.collectAsState()
    val email by globalSettingsManager.userEmail.collectAsState("")
    val isPro by globalSettingsManager.isPro.collectAsState(globalSettingsManager.currentIsPro())
    val scope = rememberCoroutineScope()
    val billing = remember { BillingRepository() }
    var confirmSignOut by remember { mutableStateOf(false) }
    val signedIn = authState is AuthState.LoggedIn

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (signedIn) {
                SettingsRow(Icons.Filled.AccountCircle, email.ifBlank { stringResource(Res.string.settings_account_signed_out) })
                SettingsRow(
                    icon = if (isPro) Icons.Filled.WorkspacePremium else Icons.Filled.Star,
                    title = stringResource(if (isPro) Res.string.settings_pro_active else Res.string.settings_pro_upgrade),
                    onClick = if (isPro) null else ({ ProPaywall.open(ProPaywall.SOURCE_SETTINGS) }),
                )
                if (isPro) {
                    SettingsRow(Icons.Filled.Lock, stringResource(Res.string.settings_manage_subscription)) {
                        scope.launch { billing.createPortal()?.overview?.let { openInSystemBrowser(it) } }
                    }
                }
                SettingsRow(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.settings_sign_out)) { confirmSignOut = true }
            } else {
                SettingsRow(Icons.Filled.AccountCircle, stringResource(Res.string.settings_sign_in)) {
                    globalAuthManager.signIn(ProPaywall.SOURCE_SETTINGS)
                }
                SettingsRow(Icons.Filled.Star, stringResource(Res.string.settings_pro_upgrade)) {
                    ProPaywall.open(ProPaywall.SOURCE_SETTINGS)
                }
            }
            // 隐私、条款、版本号收进关于页（appsettings-kmp 的 AboutContent），这里只留入口
            SettingsRow(Icons.Filled.Info, stringResource(Res.string.settings_about), onClick = onNavigateToAbout)
        }
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

@Composable
private fun SettingsRow(icon: ImageVector, title: String, onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
