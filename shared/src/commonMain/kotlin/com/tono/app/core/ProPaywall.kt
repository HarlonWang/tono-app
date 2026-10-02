package com.tono.app.core

import com.tono.app.data.local.globalSettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 订阅页统一入口。chat 库没有导航栈，只能靠总线，由根部 App 收集 [requests] 进订阅页。 */
object ProPaywall {
    const val SOURCE_SETTINGS = "settings"

    private val _requests = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val requests: SharedFlow<String> = _requests.asSharedFlow()

    fun open(source: String) {
        _requests.tryEmit(source)
    }
}

/** Paddle 收银台外跳 + 权益回流对账。权益要等 webhook 落库，必须轮询而不是查一次。 */
object ProCheckout {
    private val RECONCILE_WINDOW = 30.minutes
    private val RETRY_DELAYS = listOf(0.seconds, 3.seconds, 8.seconds, 15.seconds)

    fun markOpened() {
        globalSettingsManager.setCheckoutOpenedAt(Clock.System.now().toEpochMilliseconds())
    }

    fun shouldReconcile(): Boolean {
        val openedAt = globalSettingsManager.currentCheckoutOpenedAt()
        return openedAt > 0 &&
            Clock.System.now().toEpochMilliseconds() - openedAt < RECONCILE_WINDOW.inWholeMilliseconds
    }

    /** 返回第几次尝试拿到权益（1 起）；未到账返回 null，此时窗口刻意不清，付款可能只是没走完。 */
    suspend fun reconcile(refreshPro: suspend () -> Boolean): Int? {
        RETRY_DELAYS.forEachIndexed { index, wait ->
            if (wait > 0.seconds) delay(wait)
            val pro = try {
                refreshPro()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (pro) return index + 1
        }
        return null
    }

    fun markActivated() {
        globalSettingsManager.clearCheckoutOpenedAt()
        _activated.tryEmit(Unit)
    }

    private val _activated = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val activated: SharedFlow<Unit> = _activated.asSharedFlow()

    fun consumeActivated() {
        _activated.resetReplayCache()
    }
}
