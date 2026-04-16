package com.example.archshowcase.di

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.isDebug
import com.example.archshowcase.core.trace.verification.NetworkRecorderBridge
import com.example.archshowcase.core.trace.verification.NetworkTape
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.auth.AuthService
import com.example.archshowcase.devicetoken.DeviceTokenService
import com.example.archshowcase.network.NetworkRecorder
import com.example.archshowcase.network.api.DefaultImageApi
import com.example.archshowcase.network.api.DefaultUserApi
import com.example.archshowcase.network.api.ImageApi
import com.example.archshowcase.network.api.UserApi
import com.example.archshowcase.network.header.HeaderProvider
import com.example.archshowcase.network.interceptor.interceptSign
import com.example.archshowcase.network.interceptor.interceptTapeReplay
import com.example.archshowcase.network.mock.MockImageApi
import com.example.archshowcase.network.mock.MockUserApi
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import io.ktor.client.plugins.logging.Logger as KtorLogger

private const val TAG_KTOR = "Ktor"

/**
 * 网络模块 - HttpClient 和 Api
 */
val networkModule = module {
    singleOf(::HeaderProvider)

    // 立即注册桥接（不依赖 HttpClient 懒加载）
    NetworkRecorderBridge.register(object : NetworkRecorderBridge.Delegate {
        override fun markStart() = NetworkRecorder.markStart()
        override fun markEnd(): NetworkTape = NetworkRecorder.markEnd()
    })

    single {
        val authService: AuthService = get()
        val headerProvider: HeaderProvider = get()
        val deviceTokenService: DeviceTokenService = get()

        val configure: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            install(Logging) {
                logger = object : KtorLogger {
                    override fun log(message: String) {
                        Log.d(TAG_KTOR) { message }
                    }
                }
                level = if (isDebug()) LogLevel.BODY else LogLevel.NONE
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }

            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, response ->
                    !AppRuntimeState.verificationMode && response.status.value in 500..599
                }
                retryOnExceptionIf { _, _ ->
                    !AppRuntimeState.verificationMode
                }
                exponentialDelay()
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        authService.getAccessToken()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { BearerTokens(it, "") }
                    }
                    refreshTokens {
                        authService.refreshToken().fold(
                            onSuccess = { BearerTokens(it.accessToken, it.refreshToken) },
                            onFailure = { null }
                        )
                    }
                }
            }

            install(ContentEncoding) {
                gzip()
            }

            install(HttpCache)

            // NetworkRecorder: debug 模式下录制网络请求
            if (AppConfig.enableRestore) {
                install(NetworkRecorder.plugin)
            }

            HttpResponseValidator {
                validateResponse { response ->
                    val statusCode = response.status.value
                    if (statusCode >= 300 && statusCode != 401) {
                        throw ResponseException(response, response.bodyAsText())
                    }
                }
            }

            defaultRequest {
                headerProvider.getHeaders().forEach { (k, v) -> header(k, v) }
            }
        }

        val client = HttpClient(configure)

        // 拦截器链（HttpSend 只支持单个 intercept，所有拦截逻辑合并在此）
        // 扩展拦截器时：在此 intercept 内按顺序调用独立拦截函数
        client.also {
            it.plugin(HttpSend).intercept { request ->
                interceptTapeReplay(request)?.let { call -> return@intercept call }
                interceptSign(request, deviceTokenService)
                // 录制期间跳过缓存，确保每次请求都拿到完整响应并被录到
                if (NetworkRecorder.isCurrentlyRecording()) {
                    request.headers.remove("If-None-Match")
                    request.headers.remove("If-Modified-Since")
                    request.headers.append("Cache-Control", "no-cache, no-store")
                }
                try {
                    execute(request)
                } catch (e: Exception) {
                    if (NetworkRecorder.isCurrentlyRecording()) {
                        NetworkRecorder.recordFailure(
                            method = request.method.value,
                            url = request.url.buildString(),
                            error = e.message ?: "Unknown error"
                        )
                    }
                    throw e
                }
            }
        }
    }

    single<ImageApi> {
        if (AppRuntimeState.isInPreview) MockImageApi() else DefaultImageApi()
    }

    single<UserApi> {
        if (AppRuntimeState.isInPreview) MockUserApi() else DefaultUserApi()
    }
}
