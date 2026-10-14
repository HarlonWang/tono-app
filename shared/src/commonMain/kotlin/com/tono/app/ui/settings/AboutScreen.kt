package com.tono.app.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tono.app.core.platform.getAppVersion
import com.tono.app.resources.Res
import com.tono.app.resources.app_name
import com.tono.app.resources.common_back
import com.tono.app.resources.settings_about
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.about.AboutContent
import wang.harlon.appsettings.about.AboutLink

const val PRIVACY_URL = "https://tonote.app/privacy"
const val TERMS_URL = "https://tonote.app/terms"

/** 关于页：骨架与内置行在 appsettings-kmp，链接默认用系统浏览器打开。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        AboutContent(
            name = stringResource(Res.string.app_name),
            version = remember { getAppVersion() },
            modifier = Modifier.padding(padding),
        ) {
            group {
                link(AboutLink.Privacy(PRIVACY_URL))
                link(AboutLink.Terms(TERMS_URL))
            }
        }
    }
}
