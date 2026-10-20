package com.tono.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tono.app.auth.LoginbaseAuthManager
import com.tono.app.core.platform.isIosPlatform
import com.tono.app.resources.Res
import com.tono.app.resources.delete_account_blocked
import com.tono.app.resources.delete_account_blocked_subscription
import com.tono.app.resources.delete_account_code_sent_to
import com.tono.app.resources.delete_account_confirm
import com.tono.app.resources.delete_account_send_code
import com.tono.app.resources.delete_account_title
import com.tono.app.resources.delete_account_verify_hint
import com.tono.app.resources.delete_account_warning
import com.tono.app.resources.login_code_expired
import com.tono.app.resources.login_code_hint
import com.tono.app.resources.login_code_invalid
import com.tono.app.resources.login_code_spam_hint
import com.tono.app.resources.login_generic_error
import com.tono.app.resources.login_resend
import com.tono.app.resources.login_resend_in
import com.tono.app.resources.login_too_many_attempts
import com.tono.app.resources.login_too_many_requests
import com.tono.app.resources.login_too_many_requests_minutes
import com.tono.app.resources.settings_manage_subscription
import com.tono.app.ui.common.TonoBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import wang.harlon.loginbase.AuthError
import wang.harlon.loginbase.LoginbaseException

private enum class Step { CONFIRM, CODE }

/** 拒删原因与服务端 tono-api 的 account.ts 对齐；未知原因给通用提示 */
private const val REASON_ACTIVE_SUBSCRIPTION = "active_subscription"

/**
 * 删号面板：说明后果 → 发码到账号邮箱 → 输码永久删除（App Store 5.1.1(v)）。
 * 有生效中的订阅时服务端拒删，这里引导先去取消。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeleteAccountSheet(
    manager: LoginbaseAuthManager,
    email: String,
    onManageSubscription: () -> Unit,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.CONFIRM) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var blockedBySubscription by remember { mutableStateOf(false) }
    var cooldown by remember { mutableStateOf(0) }

    val genericError = stringResource(Res.string.login_generic_error)
    val codeInvalid = stringResource(Res.string.login_code_invalid)
    val codeExpired = stringResource(Res.string.login_code_expired)
    val tooManyAttempts = stringResource(Res.string.login_too_many_attempts)
    val tooManyRequestsTemplate = stringResource(Res.string.login_too_many_requests)
    val tooManyRequestsMinutesTemplate = stringResource(Res.string.login_too_many_requests_minutes)
    val blockedSubscription = stringResource(Res.string.delete_account_blocked_subscription)
    val blocked = stringResource(Res.string.delete_account_blocked)

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    fun describe(e: Throwable): String = when {
        e !is LoginbaseException.Api -> genericError
        e.error == AuthError.DELETION_BLOCKED ->
            if (e.reason == REASON_ACTIVE_SUBSCRIPTION) blockedSubscription else blocked
        e.error == AuthError.INVALID_CODE -> codeInvalid
        e.error == AuthError.CODE_EXPIRED -> codeExpired
        e.error == AuthError.TOO_MANY_ATTEMPTS -> tooManyAttempts
        e.error == AuthError.TOO_MANY_REQUESTS -> {
            val secs = e.retryAfterSeconds ?: 60
            if (secs >= 60) tooManyRequestsMinutesTemplate.replace("%d", ((secs + 59) / 60).toString())
            else tooManyRequestsTemplate.replace("%d", secs.toString())
        }
        else -> genericError
    }

    fun onFailure(e: Throwable) {
        blockedBySubscription = e is LoginbaseException.Api &&
            e.error == AuthError.DELETION_BLOCKED && e.reason == REASON_ACTIVE_SUBSCRIPTION
        error = describe(e)
    }

    fun send() {
        error = null
        busy = true
        scope.launch {
            runCatching { manager.sendAccountDeletionCode() }
                .onSuccess {
                    cooldown = it.cooldownSeconds
                    step = Step.CODE
                    code = ""
                }
                .onFailure(::onFailure)
            busy = false
        }
    }

    fun delete() {
        error = null
        busy = true
        scope.launch {
            runCatching { manager.deleteAccount(code) }
                .onSuccess { onDeleted() }
                .onFailure(::onFailure)
            busy = false
        }
    }

    TonoBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        title = stringResource(Res.string.delete_account_title),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                Step.CONFIRM -> {
                    Text(stringResource(Res.string.delete_account_warning), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(Res.string.delete_account_verify_hint).replace("%s", email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Step.CODE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.delete_account_code_sent_to).replace("%s", email),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(Res.string.login_code_spam_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // 不像登录那样输满 6 位自动提交：删号不可逆，必须用户亲手点确认
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                        label = { Text(stringResource(Res.string.login_code_hint)) },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { send() }, enabled = !busy && cooldown == 0) {
                        Text(
                            if (cooldown > 0) {
                                stringResource(Res.string.login_resend_in).replace("%d", cooldown.toString())
                            } else {
                                stringResource(Res.string.login_resend)
                            },
                        )
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            // iOS 首版不放任何 Paddle 链接（App Store 3.1.1），只给文字提示
            if (blockedBySubscription && !isIosPlatform()) {
                TextButton(onClick = onManageSubscription) {
                    Text(stringResource(Res.string.settings_manage_subscription))
                }
            }

            Button(
                onClick = { if (step == Step.CONFIRM) send() else delete() },
                enabled = !busy && (step == Step.CONFIRM || code.length == 6),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError)
                } else {
                    Text(
                        stringResource(
                            if (step == Step.CONFIRM) Res.string.delete_account_send_code else Res.string.delete_account_confirm,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
