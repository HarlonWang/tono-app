package com.tono.app.core.analytics

import wang.harlon.eventbase.Event

/**
 * 事件词汇，本文件即唯一权威：加事件加在这里，不要在调用点就地发明。
 * 命名与 TrendingAI 对齐（规范见 eventbase 仓 `docs/telemetry-design.md`），两边看板的 SQL 可以通用。
 *
 * `app_opened` / `app_backgrounded` 由 eventbase-kmp 自动上报；`quota_blocked` 与成单的
 * `checkout_step(step = completed)` 由 tono-api 服务端补发，都不在这里。
 *
 * 枚举值经库的 props 归一化输出为小写名；null 属性在归一化时丢弃，调用点无需判空。
 */
sealed class AppEvent(
    override val name: String,
    override val props: Map<String, Any?> = emptyMap(),
) : Event {

    data class AiRequested(val kind: AiKind, val from: String, val imageCount: Int? = null) :
        AppEvent("ai_requested", mapOf("kind" to kind, "from" to from, "image_count" to imageCount))

    /** 与 [AiRequested] 成对，一次请求恰好一条。 */
    data class AiCompleted(
        val kind: AiKind,
        val outcome: AiOutcome,
        val durationMs: Long? = null,
        val reason: String? = null,
        val tier: String? = null,
    ) : AppEvent(
        "ai_completed",
        mapOf("kind" to kind, "outcome" to outcome, "duration_ms" to durationMs, "reason" to reason, "tier" to tier),
    )

    /** 语音录入转写前的漏斗，一次按住恰好一条；转写成功后的发送另走 [AiRequested]（from = voice）。 */
    data class VoiceInput(val outcome: VoiceInputOutcome, val durationMs: Long? = null) :
        AppEvent("voice_input", mapOf("outcome" to outcome, "duration_ms" to durationMs))

    data class AuthStarted(val action: AuthAction, val method: String, val source: String) :
        AppEvent("auth_started", mapOf("action" to action, "method" to method, "source" to source))

    /** 登录面板被直接关掉不发：没有终态的 auth_started 即为放弃。 */
    data class AuthFinished(
        val action: AuthAction,
        val outcome: AuthOutcome,
        val method: String,
        val reason: String? = null,
    ) : AppEvent(
        "auth_finished",
        mapOf("action" to action, "outcome" to outcome, "method" to method, "reason" to reason),
    )

    data object SignedOut : AppEvent("signed_out")

    data class UpsellClicked(val source: String, val target: UpsellTarget) :
        AppEvent("upsell_clicked", mapOf("source" to source, "target" to target))

    data class CheckoutStep(val step: CheckoutStepKind, val plan: String? = null, val attempt: Int? = null) :
        AppEvent("checkout_step", mapOf("step" to step, "plan" to plan, "attempt" to attempt))
}

enum class AiKind { CHAT }

enum class AiOutcome { OK, ERROR, INTERRUPTED }

enum class VoiceInputOutcome { SENT, CANCELLED, TOO_SHORT, EMPTY, ERROR, PERMISSION_DENIED, PRO_GATE }

enum class AuthAction { SIGN_IN }

enum class AuthOutcome { SUCCESS, ERROR }

enum class UpsellTarget { PRO }

/** completed 由服务端在 webhook 落库时补发，客户端只有前后两段。 */
enum class CheckoutStepKind { OPENED, RECONCILED }
