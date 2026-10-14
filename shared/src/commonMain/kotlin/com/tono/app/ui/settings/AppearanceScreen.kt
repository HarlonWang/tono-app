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
import androidx.compose.ui.Modifier
import com.tono.app.resources.Res
import com.tono.app.resources.common_back
import com.tono.app.resources.settings_appearance
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.appearance.AppearanceContent
import wang.harlon.appsettings.appearance.LocalAppearanceState

/** 外观页：Tono 用 Fixed 色源，这里只有深浅模式一节。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        AppearanceContent(LocalAppearanceState.current, modifier = Modifier.padding(padding))
    }
}
