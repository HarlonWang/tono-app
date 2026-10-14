package com.tono.app.chat

import androidx.compose.runtime.Composable
import com.tono.app.auth.AuthState
import com.tono.app.auth.globalAuthManager
import com.tono.app.core.ProPaywall
import com.tono.app.core.analytics.AiKind
import com.tono.app.core.analytics.AiOutcome
import com.tono.app.core.analytics.AppEvent
import com.tono.app.core.analytics.VoiceInputOutcome
import com.tono.app.core.analytics.track
import com.tono.app.core.platform.openInSystemBrowser
import com.tono.app.core.platform.requestLanguage
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.remote.API_HOST
import com.tono.app.data.remote.installTonoAuth
import com.tono.app.data.remote.trackAuthTokenCache
import com.tono.app.ui.common.ProBadge
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import wang.harlon.chatbase.host.ChatAiEvent
import wang.harlon.chatbase.host.ChatAiOutcome
import wang.harlon.chatbase.host.ChatHost
import wang.harlon.chatbase.host.ChatVoiceOutcome
import wang.harlon.chatbase.host.chatHost

const val REPORT_IMAGE_URL = "https://tonote.app/report"

private object TonoChatHost : ChatHost {
    override val apiBaseUrl = "$API_HOST/api"
    override val canSignIn: Boolean get() = globalAuthManager.isSupported
    override fun isLoggedInNow() = globalAuthManager.authState.value is AuthState.LoggedIn

    // 收集时才解析 globalAuthManager：初始化期求值会把 Noop 的 LoggedOut 永久钉死
    override val isLoggedIn: Flow<Boolean> = flow {
        emitAll(globalAuthManager.authState.map { it is AuthState.LoggedIn })
    }

    override fun signIn(source: String) = globalAuthManager.signIn(source)
    override fun openPaywall(source: String) = ProPaywall.open(source)
    override fun reportGeneratedImage(imageUrl: String) = openInSystemBrowser(REPORT_IMAGE_URL)

    override fun currentIsPro() = globalSettingsManager.currentIsPro()
    override val isPro: Flow<Boolean> get() = globalSettingsManager.isPro

    override val chatModelChoice: Flow<String> get() = globalSettingsManager.chatModelChoice
    override fun currentChatModelChoice() = globalSettingsManager.currentChatModelChoice()
    override fun pinChatModel(id: String) = globalSettingsManager.pinChatModel(id)
    override fun followServerDefault() = globalSettingsManager.followServerDefault()

    override fun imagesMaxCount() = globalSettingsManager.chatImagesMaxCount()
    override fun imagesPerImageJpegKb() = globalSettingsManager.chatImagesPerImageJpegKb()
    override fun voiceMaxDurationMs() = globalSettingsManager.chatVoiceMaxDurationMs()
    override fun installId() = globalSettingsManager.getOrCreateInstallId()
    override suspend fun requestLang(): String = requestLanguage()

    override fun configureHttpAuth(config: HttpClientConfig<*>) = config.installTonoAuth()
    override fun registerAuthorizedClient(client: HttpClient) {
        client.trackAuthTokenCache()
    }

    override fun onAiEvent(event: ChatAiEvent) = track(
        when (event) {
            is ChatAiEvent.Requested -> AppEvent.AiRequested(AiKind.CHAT, from = event.from, imageCount = event.imageCount)
            is ChatAiEvent.Completed -> AppEvent.AiCompleted(
                kind = AiKind.CHAT,
                outcome = when (event.outcome) {
                    ChatAiOutcome.OK -> AiOutcome.OK
                    ChatAiOutcome.ERROR -> AiOutcome.ERROR
                    ChatAiOutcome.INTERRUPTED -> AiOutcome.INTERRUPTED
                },
                durationMs = event.durationMs,
                reason = event.reason,
                tier = event.tier,
            )
            is ChatAiEvent.VoiceInput -> AppEvent.VoiceInput(
                outcome = when (event.outcome) {
                    ChatVoiceOutcome.SENT -> VoiceInputOutcome.SENT
                    ChatVoiceOutcome.CANCELLED -> VoiceInputOutcome.CANCELLED
                    ChatVoiceOutcome.TOO_SHORT -> VoiceInputOutcome.TOO_SHORT
                    ChatVoiceOutcome.EMPTY -> VoiceInputOutcome.EMPTY
                    ChatVoiceOutcome.ERROR -> VoiceInputOutcome.ERROR
                    ChatVoiceOutcome.PERMISSION_DENIED -> VoiceInputOutcome.PERMISSION_DENIED
                    ChatVoiceOutcome.PRO_GATE -> VoiceInputOutcome.PRO_GATE
                },
                durationMs = event.durationMs,
            )
        },
    )
    override val proBadge: (@Composable () -> Unit) = { ProBadge() }
}

fun installTonoChatHost() {
    chatHost = TonoChatHost
}
