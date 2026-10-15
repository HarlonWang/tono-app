package com.tono.app.core

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.tono.app.auth.AuthState
import com.tono.app.auth.globalAuthManager
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.remote.AppConfig
import com.tono.app.data.remote.UserRepository
import com.tono.app.resources.Res
import com.tono.app.resources.chat_suggest_what_can_you_do
import com.tono.app.resources.chat_suggest_what_can_you_do_prompt
import com.tono.app.resources.settings_title
import com.tono.app.ui.auth.LoginSheetHost
import com.tono.app.ui.settings.SettingsScreen
import com.tono.app.ui.subscription.SubscriptionScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import wang.harlon.chatbase.model.ChatModelsProvider
import wang.harlon.chatbase.ui.ChatScreen
import wang.harlon.chatbase.ui.ChatSuggestion
import com.tono.app.data.local.globalSettings
import wang.harlon.appsettings.AboutScreen
import wang.harlon.appsettings.AppSettingsRoot
import wang.harlon.appsettings.AppearanceScreen
import wang.harlon.appsettings.rememberAppSettings
import wang.harlon.appsettings.appearance.ColorSource
import wang.harlon.appsettings.ui.SettingsPage
import com.tono.app.ui.theme.TonoLightColors
import com.tono.app.ui.theme.TonoDarkColors
import com.tono.app.ui.settings.PRIVACY_URL
import com.tono.app.ui.settings.TERMS_URL
import com.tono.app.resources.app_name

sealed interface Route
data object Chat : Route
data object Settings : Route
data object About : Route
data object Appearance : Route
data object ProSubscription : Route

/** 栈底（Chat）永不弹出：转场期间连按返回会把栈弹空，NavDisplay 抛 backstack cannot be empty。 */
private fun MutableList<Route>.safePop() {
    if (size > 1) removeAt(lastIndex)
}

@Composable
fun App() {
    val backStack = remember { mutableStateListOf<Route>(Chat) }

    // syncMe 是 isPro 的日常唯一写入点，挂根部让登录态一变就触发，不受页面切换影响
    val authState by globalAuthManager.authState.collectAsState()
    val userRepository = remember { UserRepository() }
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) userRepository.syncMe()
    }
    LaunchedEffect(Unit) {
        AppConfig.refresh()
        ChatModelsProvider.warmUp(this)
    }

    // 设置层的全部配置：品牌黑白色写死（theme-design.md）只开放深浅模式，关于页两条链接，升级后首启弹更新说明
    val appSettings = rememberAppSettings {
        settings = globalSettings
        appearance {
            colorSource = ColorSource.Fixed(light = TonoLightColors, dark = TonoDarkColors)
            allowCustom = false
            allowSystemDynamic = false
        }
        about {
            name = stringResource(Res.string.app_name)
            privacy = PRIVACY_URL
            terms = TERMS_URL
        }
        whatsNew { bundled = { Res.readBytes("files/whatsnew.json").decodeToString() } }
    }
    AppSettingsRoot(appSettings) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LoginSheetHost()
            ProReconcileHost(userRepository)
            LaunchedEffect(Unit) {
                ProPaywall.requests.collect {
                    if (backStack.lastOrNull() !is ProSubscription) backStack.add(ProSubscription)
                }
            }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.safePop() },
                transitionSpec = {
                    slideInHorizontally { it / 8 } + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                },
                popTransitionSpec = {
                    slideInHorizontally { -it / 8 } + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally { -it / 8 } + fadeIn(tween(200)) togetherWith
                        slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                },
                entryProvider = { key ->
                    when (key) {
                        is Chat -> NavEntry(key) {
                            ChatScreen(
                                onBack = {},
                                navigationIcon = {
                                    IconButton(onClick = { backStack.add(Settings) }) {
                                        Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.settings_title))
                                    }
                                },
                                suggestions = listOf(
                                    ChatSuggestion(
                                        label = stringResource(Res.string.chat_suggest_what_can_you_do),
                                        prompt = stringResource(Res.string.chat_suggest_what_can_you_do_prompt),
                                    ),
                                ),
                            )
                        }
                        is Settings -> NavEntry(key) {
                            SettingsScreen(
                                onBack = { backStack.safePop() },
                                onOpen = { page ->
                                    backStack.add(when (page) { SettingsPage.Appearance -> Appearance; SettingsPage.About -> About })
                                },
                            )
                        }
                        is About -> NavEntry(key) { AboutScreen(onBack = { backStack.safePop() }) }
                        is Appearance -> NavEntry(key) { AppearanceScreen(onBack = { backStack.safePop() }) }
                        is ProSubscription -> NavEntry(key) { SubscriptionScreen(onBack = { backStack.safePop() }) }
                    }
                },
            )
        }
    }
}

/** 回前台对账 Pro：只在已登录、非 Pro、且处于收银台对账窗口内才查。用户付完款回来停在哪页无法预期，故挂根部。 */
@Composable
private fun ProReconcileHost(userRepository: UserRepository) {
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        if (globalAuthManager.authState.value is AuthState.LoggedIn &&
            !globalSettingsManager.currentIsPro() && ProCheckout.shouldReconcile()
        ) {
            scope.launch {
                val attempt = ProCheckout.reconcile {
                    userRepository.syncMe() != null && globalSettingsManager.currentIsPro()
                }
                if (attempt != null) ProCheckout.markActivated(attempt)
            }
        }
        onPauseOrDispose { }
    }
}
