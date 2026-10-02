package com.tono.app.data.remote

import com.tono.app.auth.AuthManager
import com.tono.app.auth.globalAuthManager
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private val authorizedClients = MutableStateFlow<List<HttpClient>>(emptyList())

/**
 * 给自家 API 的 client 装鉴权，接法见 loginbase-kmp README 第 3 步。
 * 刷新必须走 [AuthManager.refreshAccessToken]：ktor 插件的单飞只覆盖单个 client，
 * 自己 POST /auth/refresh 会并发刷新、烧掉服务端的救活配额且全程无报错。
 */
fun HttpClientConfig<*>.installTonoAuth(authManager: () -> AuthManager = { globalAuthManager }) {
    install(Auth) {
        bearer {
            loadTokens { authManager().getAccessToken()?.let { BearerTokens(it, null) } }
            refreshTokens { authManager().refreshAccessToken()?.let { BearerTokens(it, null) } }
        }
    }
}

fun HttpClient.trackAuthTokenCache(): HttpClient = also { client ->
    authorizedClients.update { it + client }
}

/** 登出与会话终结时必调：插件缓存的是上一条会话的 token，不清会继续拿它发请求。 */
fun clearAuthTokenCache() {
    authorizedClients.value.forEach { it.authProvider<BearerAuthProvider>()?.clearToken() }
}
