package com.tono.app.auth

import com.tono.app.core.analytics.AppEvent
import com.tono.app.core.analytics.AuthAction
import com.tono.app.core.analytics.setAnalyticsUser
import com.tono.app.core.analytics.track
import com.tono.app.core.platform.getAppVersion
import com.tono.app.core.platform.getDeviceInfo
import com.tono.app.core.platform.isIosPlatform
import com.tono.app.core.platform.requestLanguage
import com.tono.app.data.local.globalSettingsManager
import com.tono.app.data.remote.API_HOST
import com.tono.app.data.remote.clearAuthTokenCache
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wang.harlon.eventbase.Eventbase
import wang.harlon.loginbase.AuthClient
import wang.harlon.loginbase.AuthState as LoginbaseState
import wang.harlon.loginbase.ClientInfo
import wang.harlon.loginbase.ClientPlatform
import wang.harlon.loginbase.RefreshOutcome
import wang.harlon.loginbase.TokenStore

sealed interface AuthState {
    /** 会话尚未从存储恢复，不等于未登录：据此下匿名结论并写进 UI 的地方要先等它落定。 */
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data object LoggedIn : AuthState
}

interface AuthManager {
    val isSupported: Boolean
    val authState: StateFlow<AuthState>

    /** 请求登录：发布到 [LoginSheetBus]，由 App 根部弹登录面板。 */
    fun signIn(source: String)

    fun signOut()

    /** 业务代码不该调它，鉴权由 [com.tono.app.data.remote.installTonoAuth] 统一处理。 */
    suspend fun getAccessToken(): String?

    suspend fun refreshAccessToken(): String? = null
}

object NoopAuthManager : AuthManager {
    override val isSupported: Boolean = false
    override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.LoggedOut)
    override fun signIn(source: String) {}
    override fun signOut() {}
    override suspend fun getAccessToken(): String? = null
}

var globalAuthManager: AuthManager = NoopAuthManager

/** 登录面板请求总线。StateFlow 而非事件流：「当前是否有待处理的登录请求」是状态，重建后收集者能立刻恢复。 */
object LoginSheetBus {
    private val _request = MutableStateFlow<String?>(null)
    val request: StateFlow<String?> = _request

    fun request(source: String) {
        _request.value = source
        // 开一条 flow 串起本次登录，终态 auth_finished 用 Eventbase.currentFlow() 接回同一个漏斗
        track(AppEvent.AuthStarted(AuthAction.SIGN_IN, method = "email", source = source), Eventbase.startFlow())
    }

    fun clear() {
        _request.value = null
    }
}

/**
 * AuthClient 的单飞刷新锁是实例字段，全进程必须只有一个实例，由 [initLoginbaseAuth] 保证。
 */
class LoginbaseAuthManager(
    val client: AuthClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : AuthManager {

    override val isSupported: Boolean = true

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        scope.launch {
            client.restore()
            client.authState.collect { state ->
                if (state is LoginbaseState.SignedOut) {
                    // 挂在这里而不是只挂 signOut()：会话被动失效不走那条路，不清 user_id 会一直带在之后的匿名事件上
                    setAnalyticsUser(null)
                    clearAuthTokenCache()
                }
                _authState.value = when (state) {
                    LoginbaseState.Unknown -> AuthState.Unknown
                    is LoginbaseState.SignedOut -> AuthState.LoggedOut
                    // RefreshFailed 不是登出（库文档硬性要求），多半只是弱网
                    LoginbaseState.SignedIn, is LoginbaseState.RefreshFailed -> AuthState.LoggedIn
                }
            }
        }
    }

    override fun signIn(source: String) = LoginSheetBus.request(source)

    override fun signOut() {
        scope.launch {
            // 本地清理排在 client.signOut() 前：后者要等 DELETE /sessions 返回
            clearLocalUser()
            track(AppEvent.SignedOut)
            setAnalyticsUser(null)
            client.signOut()
        }
    }

    /** 删号第一步：往账号邮箱发删号码。服务端拒删时抛 [wang.harlon.loginbase.AuthError.DELETION_BLOCKED]。 */
    suspend fun sendAccountDeletionCode() = client.sendAccountDeletionCode()

    /**
     * 删号第二步：服务端成功后库已清掉会话（authState 收集处随之清埋点 user），这里补齐 App 自己的本地状态。
     * 失败时抛出、保持登录态，由调用方展示错误。
     */
    suspend fun deleteAccount(code: String) {
        client.deleteAccount(code)
        track(AppEvent.AccountDeleted)
        clearLocalUser()
    }

    private fun clearLocalUser() {
        globalSettingsManager.setUserEmail(null)
        globalSettingsManager.setIsPro(false)
        globalSettingsManager.followServerDefault()
    }

    override suspend fun getAccessToken(): String? = client.accessToken()

    override suspend fun refreshAccessToken(): String? =
        when (val outcome = client.refresh()) {
            is RefreshOutcome.Success -> outcome.tokens.accessToken
            else -> null
        }
}

private fun loginbaseClientInfo(): ClientInfo? = runCatching {
    ClientInfo(
        appName = "Tono",
        version = getAppVersion(),
        platform = if (isIosPlatform()) ClientPlatform.IOS else ClientPlatform.ANDROID,
        deviceInfo = getDeviceInfo(),
    )
}.getOrNull()

/**
 * 进程级初始化，幂等。传入的 TokenStore 必须同步落盘（服务端「丢回执救活」的配套硬要求），
 * 别改用 App 自己那份异步 apply 的 Settings。
 */
fun initLoginbaseAuth(
    tokenStore: TokenStore,
    httpEngine: () -> HttpClientEngine? = { null },
): LoginbaseAuthManager {
    (globalAuthManager as? LoginbaseAuthManager)?.let { return it }
    val client = AuthClient("$API_HOST/auth", tokenStore) {
        this.httpEngine = httpEngine()
        // 验证码邮件语言：iOS 上 Compose 资源不进 .lproj，库的平台推断恒为 en，故显式给
        localeProvider = { requestLanguage() }
        this.client = loginbaseClientInfo()
    }
    val manager = LoginbaseAuthManager(client)
    globalAuthManager = manager
    return manager
}
