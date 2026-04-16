package com.example.archshowcase.network.interceptor

import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.network.VerificationTapeHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TAG = "TapeReplay"

/**
 * VF tape 耗尽或离线时抛出的异常
 *
 * 携带原始错误信息，使验证截图与录制截图一致。
 */
class VfTapeException(message: String) : Exception(message)

/**
 * Tape 回放拦截器
 *
 * 验证模式下从录制的 NetworkTape 返回响应，不走真实网络。
 * - 成功录制：MockEngine 回放响应
 * - 失败录制：抛出 VfTapeException（携带录制时的原始错误信息）
 * - tape 耗尽：抛出 VfTapeException（阻止 fallthrough 到真实网络）
 *
 * @return 回放的 HttpClientCall，或 null 表示不拦截（交给下一个拦截器）
 */
suspend fun interceptTapeReplay(request: HttpRequestBuilder): HttpClientCall? {
    val tape = VerificationTapeHolder.tape
    if (!AppRuntimeState.verificationMode || tape == null) return null

    val reqMethod = request.method.value
    val reqUrl = request.url.build().toString()
    val recording = VerificationTapeHolder.nextRecording(reqMethod, reqUrl) ?: run {
        Log.w(TAG) { "Tape exhausted for $reqMethod $reqUrl" }
        throw VfTapeException("VF tape exhausted: no recording for $reqMethod $reqUrl")
    }

    // 回放失败录制：抛出与录制时相同的错误
    if (recording.failed) {
        val errorMsg = recording.errorMessage ?: "Network unavailable (VF offline)"
        Log.d(TAG) { "Replaying failure [${recording.sequence}] ${recording.method} ${recording.url} → $errorMsg" }
        throw VfTapeException(errorMsg)
    }

    Log.d(TAG) { "Replaying [${recording.sequence}] ${recording.method} ${recording.url} → ${recording.responseStatus}" }

    val replayEngine = MockEngine { _ ->
        respond(
            content = recording.responseBody,
            status = HttpStatusCode.fromValue(recording.responseStatus),
            headers = headersOf("Content-Type" to listOf("application/json"))
        )
    }
    val replayClient = HttpClient(replayEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    val replayUrl = request.url.build()
    val replayMethod = request.method
    return replayClient.request(replayUrl) { method = replayMethod }.call
}
