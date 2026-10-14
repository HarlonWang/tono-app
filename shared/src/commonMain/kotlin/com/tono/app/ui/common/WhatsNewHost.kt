package com.tono.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tono.app.core.platform.getAppVersion
import com.tono.app.core.platform.getSystemLanguage
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.resources.Res
import com.tono.app.resources.whats_new_got_it
import com.tono.app.resources.whats_new_title
import com.tono.app.update.WhatsNewInfo
import com.tono.app.update.parseWhatsNew
import com.tono.app.update.pickByLanguage
import com.tono.app.update.shouldShowWhatsNew
import org.jetbrains.compose.resources.stringResource

/** 挂在 App 根部：升级后首次启动弹一次「新版本更新说明」，内容来自随包内置的 whatsnew.json。 */
@Composable
fun WhatsNewHost() {
    var info by remember { mutableStateOf<WhatsNewInfo?>(null) }
    val current = remember { getAppVersion() }

    LaunchedEffect(Unit) {
        val lastSeen = globalSettingsManager.getLastSeenWhatsNewVersion()
        if (lastSeen == null) {
            // 首次安装：静默记录当前版本，不弹
            globalSettingsManager.setLastSeenWhatsNewVersion(current)
            return@LaunchedEffect
        }
        if (lastSeen == current) return@LaunchedEffect

        val bundled = runCatching { Res.readBytes("files/whatsnew.json").decodeToString() }
            .getOrNull()?.let(::parseWhatsNew)
        if (bundled != null && !bundled.isEmpty && shouldShowWhatsNew(current, bundled.version, lastSeen)) {
            info = bundled
        } else {
            // dev 构建 / CI 生成失败等不满足弹出条件：直接标记已看，避免一直 pending
            globalSettingsManager.setLastSeenWhatsNewVersion(current)
        }
    }

    info?.let { whatsNew ->
        val items = remember(whatsNew) { pickByLanguage(getSystemLanguage(), whatsNew.zh, whatsNew.en) }
        val dismiss = {
            globalSettingsManager.setLastSeenWhatsNewVersion(current)
            info = null
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(Res.string.whats_new_title, whatsNew.version)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { Text("• $it") }
                }
            },
            confirmButton = {
                TextButton(onClick = dismiss) { Text(stringResource(Res.string.whats_new_got_it)) }
            },
        )
    }
}
