package com.tono.app.data.remote

import com.tono.app.core.analytics.setAnalyticsUser
import com.tono.app.core.billing.AppStoreBilling
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.model.AppConfigResponse
import com.tono.app.data.model.CheckoutResponse
import com.tono.app.data.model.MeUser
import com.tono.app.data.model.PortalResponse
import com.tono.app.data.model.PricesResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 各仓库失败一律返回 null；CancellationException 必须原样抛出，吞掉会让协程取消传不出去

class UserRepository(private val api: TonoApi = TonoApi()) {
    /** 拉 /api/me 并把 email、pro 落到本地；pro 的日常唯一写入点。 */
    suspend fun syncMe(): MeUser? = try {
        val me = api.fetchMe()
        globalSettingsManager.setUserEmail(me.user.email)
        globalSettingsManager.setIsPro(me.pro)
        setAnalyticsUser(me.user.userId)
        AppStoreBilling.identify(me.user.userId)
        me.user
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

class BillingRepository(private val api: TonoApi = TonoApi()) {
    suspend fun fetchPrices(): PricesResponse? = try {
        api.fetchPrices()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun createCheckout(plan: String): CheckoutResponse? = try {
        api.createCheckout(plan, globalSettingsManager.getOrCreateInstallId())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** 生效中 Pro 的来源："apple" / "paddle"，无订阅或拉取失败为 null。 */
    suspend fun fetchSubscriptionSource(): String? = try {
        api.fetchSubscription().source
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun createPortal(): PortalResponse? = try {
        api.createPortalSession()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

object AppConfig {
    private val _current = MutableStateFlow<AppConfigResponse?>(null)
    /** 本次进程拉到的配置；null 表示还没拉到，付费墙与额度说明按本地默认文案渲染。 */
    val current: StateFlow<AppConfigResponse?> = _current

    /** 冷启动拉一次，fail-open：拉不到就用本地缓存的 chat 参数。 */
    suspend fun refresh(api: TonoApi = TonoApi()): AppConfigResponse? = try {
        val config = api.fetchAppConfig()
        globalSettingsManager.setChatImagesConfig(config.chatImages?.maxCount, config.chatImages?.perImageJpegKb)
        globalSettingsManager.setChatVoiceConfig(config.chatVoice?.maxDurationMs)
        _current.value = config
        config
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
