package com.tono.app.data.local

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getStringFlow
import kotlinx.coroutines.flow.Flow
import wang.harlon.chatbase.model.FOLLOW_SERVER_DEFAULT
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val INSTALL_ID_KEY = "prefs_install_id"
private const val IS_PRO_KEY = "prefs_is_pro"
private const val USER_EMAIL_KEY = "prefs_user_email"
private const val CHAT_MODEL_KEY = "prefs_chat_model"
private const val CHAT_IMAGES_MAX_KEY = "prefs_chat_images_max"
private const val CHAT_IMAGES_PER_KB_KEY = "prefs_chat_images_per_kb"
private const val CHAT_VOICE_MAX_MS_KEY = "prefs_chat_voice_max_ms"
private const val CHECKOUT_OPENED_AT_KEY = "prefs_checkout_opened_at"

class SettingsManager(private val settings: ObservableSettings) {

    /** 安装标识：埋点、chat 的 X-Install-Id、checkout 的 custom_data 共用同一个值，否则服务端事件与客户端漏斗串不起来。 */
    @OptIn(ExperimentalUuidApi::class)
    private val installId: String by lazy {
        settings.getStringOrNull(INSTALL_ID_KEY)?.let { return@lazy it }
        val id = Uuid.random().toString()
        settings.putString(INSTALL_ID_KEY, id)
        id
    }

    fun getOrCreateInstallId(): String = installId

    val isPro: Flow<Boolean> = settings.getBooleanFlow(IS_PRO_KEY, false)
    fun currentIsPro(): Boolean = settings.getBoolean(IS_PRO_KEY, false)
    fun setIsPro(value: Boolean) = settings.putBoolean(IS_PRO_KEY, value)

    val userEmail: Flow<String> = settings.getStringFlow(USER_EMAIL_KEY, "")
    fun setUserEmail(value: String?) {
        if (value == null) settings.remove(USER_EMAIL_KEY) else settings.putString(USER_EMAIL_KEY, value)
    }

    val chatModelChoice: Flow<String> = settings.getStringFlow(CHAT_MODEL_KEY, FOLLOW_SERVER_DEFAULT)
    fun currentChatModelChoice(): String = settings.getString(CHAT_MODEL_KEY, FOLLOW_SERVER_DEFAULT)
    fun pinChatModel(id: String) = settings.putString(CHAT_MODEL_KEY, id)
    fun followServerDefault() = settings.putString(CHAT_MODEL_KEY, FOLLOW_SERVER_DEFAULT)

    fun chatImagesMaxCount(): Int = settings.getInt(CHAT_IMAGES_MAX_KEY, 9)
    fun chatImagesPerImageJpegKb(): Int = settings.getInt(CHAT_IMAGES_PER_KB_KEY, 280)
    fun chatVoiceMaxDurationMs(): Int = settings.getInt(CHAT_VOICE_MAX_MS_KEY, 60_000)
    fun setChatImagesConfig(maxCount: Int?, perImageJpegKb: Int?) {
        maxCount?.let { settings.putInt(CHAT_IMAGES_MAX_KEY, it) }
        perImageJpegKb?.let { settings.putInt(CHAT_IMAGES_PER_KB_KEY, it) }
    }
    fun setChatVoiceConfig(maxDurationMs: Int?) {
        maxDurationMs?.let { settings.putInt(CHAT_VOICE_MAX_MS_KEY, it) }
    }

    fun currentCheckoutOpenedAt(): Long = settings.getLong(CHECKOUT_OPENED_AT_KEY, 0L)
    fun setCheckoutOpenedAt(epochMillis: Long) = settings.putLong(CHECKOUT_OPENED_AT_KEY, epochMillis)
    fun clearCheckoutOpenedAt() = settings.remove(CHECKOUT_OPENED_AT_KEY)
}

val globalSettings: ObservableSettings by lazy { Settings() as ObservableSettings }
val globalSettingsManager: SettingsManager by lazy { SettingsManager(globalSettings) }
