package com.flix.timetable.data.remote

import co.touchlab.kermit.Logger
import com.flix.timetable.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger as KtorLogger

internal val ApiJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

/** Retries for transient failures (5xx, I/O, timeouts). Delays grow exponentially from [baseDelayMillis]. */
internal data class RetryPolicy(
    val maxRetries: Int = 2,
    val baseDelayMillis: Long = 500,
) {
    companion object {
        val None = RetryPolicy(maxRetries = 0)
    }
}

private val apiKeyInUrl = Regex("(apiKey=)[^&\\s]+")

/** Logged URLs carry the API key as a query parameter: never let it reach the log. */
internal fun String.redactApiKey(): String = replace(apiKeyInUrl, "$1***")

internal fun createHttpClient(
    engine: HttpClientEngine,
    config: ApiConfig,
    logger: Logger,
    retryPolicy: RetryPolicy = RetryPolicy(),
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true

        defaultRequest { url(config.baseUrl) }

        install(ContentNegotiation) { json(ApiJson) }

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 20_000
        }

        if (retryPolicy.maxRetries > 0) {
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = retryPolicy.maxRetries)
                retryOnException(maxRetries = retryPolicy.maxRetries, retryOnTimeout = true)
                delayMillis { retry -> retryPolicy.baseDelayMillis shl (retry - 1).coerceAtLeast(0) }
            }
        }

        if (config.isDebug) {
            install(Logging) {
                level = LogLevel.INFO
                this.logger =
                    object : KtorLogger {
                        override fun log(message: String) = logger.d { message.redactApiKey() }
                    }
            }
        }
    }
