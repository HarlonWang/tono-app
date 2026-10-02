package com.tono.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tono.app.auth.LoginSheetBus
import com.tono.app.auth.LoginbaseAuthManager
import com.tono.app.auth.globalAuthManager
import com.tono.app.resources.Res
import com.tono.app.resources.login_code_expired
import com.tono.app.resources.login_code_hint
import com.tono.app.resources.login_code_invalid
import com.tono.app.resources.login_code_sent_to
import com.tono.app.resources.login_code_spam_hint
import com.tono.app.resources.login_code_title
import com.tono.app.resources.login_continue
import com.tono.app.resources.login_email_hint
import com.tono.app.resources.login_email_invalid
import com.tono.app.resources.login_generic_error
import com.tono.app.resources.login_resend
import com.tono.app.resources.login_resend_in
import com.tono.app.resources.login_signup_hint
import com.tono.app.resources.login_title
import com.tono.app.resources.login_too_many_attempts
import com.tono.app.resources.login_too_many_requests
import com.tono.app.resources.login_too_many_requests_minutes
import com.tono.app.ui.common.TonoBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import wang.harlon.loginbase.AuthError
import wang.harlon.loginbase.LoginbaseException

/** 挂在 App 根部，收 [LoginSheetBus] 的请求弹邮箱验证码登录面板。 */
@Composable
fun LoginSheetHost() {
    val source by LoginSheetBus.request.collectAsState()
    if (source == null) return
    LoginSheet(onDismiss = { LoginSheetBus.clear() })
}

private enum class Step { EMAIL, CODE }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginSheet(onDismiss: () -> Unit) {
    val manager = globalAuthManager as? LoginbaseAuthManager
    if (manager == null) {
        onDismiss()
        return
    }
    val client = manager.client
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(Step.EMAIL) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableStateOf(0) }

    val genericError = stringResource(Res.string.login_generic_error)
    val emailInvalid = stringResource(Res.string.login_email_invalid)
    val codeInvalid = stringResource(Res.string.login_code_invalid)
    val codeExpired = stringResource(Res.string.login_code_expired)
    val tooManyAttempts = stringResource(Res.string.login_too_many_attempts)
    val tooManyRequestsTemplate = stringResource(Res.string.login_too_many_requests)
    val tooManyRequestsMinutesTemplate = stringResource(Res.string.login_too_many_requests_minutes)

    // 冷却秒数用服务端给的，不写死 60
    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    fun describe(e: Throwable): String = when {
        e !is LoginbaseException.Api -> genericError
        e.error == AuthError.INVALID_EMAIL -> emailInvalid
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

    fun send() {
        error = null
        busy = true
        scope.launch {
            runCatching { client.sendCode(email.trim()) }
                .onSuccess {
                    cooldown = it.cooldownSeconds
                    step = Step.CODE
                    code = ""
                }
                .onFailure { error = describe(it) }
            busy = false
        }
    }

    fun verify() {
        error = null
        busy = true
        scope.launch {
            runCatching { client.verifyCode(email.trim(), code) }
                .onSuccess { onDismiss() }
                .onFailure { error = describe(it) }
            busy = false
        }
    }

    TonoBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(if (step == Step.EMAIL) Res.string.login_title else Res.string.login_code_title),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                Step.EMAIL -> OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.login_email_hint)) },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )

                Step.CODE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.login_code_sent_to).replace("%s", email.trim()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(Res.string.login_code_spam_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input ->
                            code = input.filter { it.isDigit() }.take(6)
                            if (code.length == 6 && !busy) verify()
                        },
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

            Button(
                onClick = { if (step == Step.EMAIL) send() else verify() },
                enabled = !busy && when (step) {
                    Step.EMAIL -> email.contains('@')
                    Step.CODE -> code.length == 6
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(Res.string.login_continue))
                }
            }

            if (step == Step.EMAIL) {
                Text(
                    text = stringResource(Res.string.login_signup_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
