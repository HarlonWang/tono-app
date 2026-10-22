package com.tono.app.data.remote

import com.tono.app.core.platform.getUserAgent
import com.tono.app.data.model.AppConfigResponse
import com.tono.app.data.model.CheckoutResponse
import com.tono.app.data.model.MeResponse
import com.tono.app.data.model.PortalResponse
import com.tono.app.data.model.PricesResponse
import com.tono.app.data.model.SubscriptionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val API_HOST = "https://api.tonote.app"

class ApiException(val statusCode: Int, message: String) : Exception(message)

open class TonoApi {
    internal companion object {
        val sharedClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
                install(DefaultRequest) {
                    header(HttpHeaders.UserAgent, getUserAgent())
                }
                installTonoAuth()
            }.trackAuthTokenCache()
        }
    }

    private val client get() = sharedClient

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (status.value !in 200..299) throw ApiException(status.value, bodyAsText())
        return body()
    }

    open suspend fun fetchAppConfig(): AppConfigResponse = client.get("$API_HOST/api/app-config").bodyOrThrow()

    open suspend fun fetchMe(): MeResponse = client.get("$API_HOST/api/me").bodyOrThrow()

    open suspend fun fetchPrices(): PricesResponse = client.get("$API_HOST/api/billing/prices").bodyOrThrow()

    open suspend fun createCheckout(plan: String, installId: String): CheckoutResponse =
        client.post("$API_HOST/api/billing/checkout") {
            contentType(ContentType.Application.Json)
            header("X-Install-Id", installId)
            setBody(buildJsonObject { put("plan", plan) })
        }.bodyOrThrow()

    open suspend fun fetchSubscription(): SubscriptionResponse =
        client.get("$API_HOST/api/billing/subscription").bodyOrThrow()

    open suspend fun createPortalSession(): PortalResponse =
        client.post("$API_HOST/api/billing/portal").bodyOrThrow()
}
