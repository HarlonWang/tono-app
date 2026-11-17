package com.tono.app.ui.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tono.app.core.platform.openInSystemBrowser
import com.tono.app.resources.Res
import com.tono.app.resources.consent_agree
import com.tono.app.resources.consent_decline_note
import com.tono.app.resources.consent_intro
import com.tono.app.resources.consent_privacy_link
import com.tono.app.resources.consent_title
import com.tono.app.resources.consent_use_body
import com.tono.app.resources.consent_use_title
import com.tono.app.resources.consent_what_body
import com.tono.app.resources.consent_what_title
import com.tono.app.resources.consent_who_body
import com.tono.app.resources.consent_who_title
import com.tono.app.resources.settings_title
import com.tono.app.ui.settings.PRIVACY_URL
import org.jetbrains.compose.resources.stringResource

/**
 * 首次进入 chat 前的 AI 数据共享同意页（App Store 5.1.1(i) / 5.1.2(i)）：写明发什么、发给谁、作何用，
 * 用户点同意前不渲染 chat，也就不可能发出任何消息。只写在隐私政策里不满足审核要求。
 * 这里列的提供方要与 tono-web 隐私政策第 3 节一致；增减提供方时同步改文案并递增 [AI_DATA_CONSENT_VERSION] 重新征求同意。
 */
@Composable
fun AiDataConsentScreen(onAgree: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // 不同意也要能进设置：已登录用户仍需管理订阅、退出或删除账号
        IconButton(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.settings_title))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.consent_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.consent_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ConsentItem(stringResource(Res.string.consent_what_title), stringResource(Res.string.consent_what_body))
            ConsentItem(stringResource(Res.string.consent_who_title), stringResource(Res.string.consent_who_body))
            ConsentItem(stringResource(Res.string.consent_use_title), stringResource(Res.string.consent_use_body))
            Spacer(Modifier.height(8.dp))
            // 去掉 TextButton 自带的水平内边距，让链接与正文左对齐
            TextButton(onClick = { openInSystemBrowser(PRIVACY_URL) }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(Res.string.consent_privacy_link))
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onAgree, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.consent_agree))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.consent_decline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConsentItem(title: String, body: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
