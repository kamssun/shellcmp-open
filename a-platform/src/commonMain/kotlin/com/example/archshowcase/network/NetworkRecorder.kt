package com.example.archshowcase.network

import com.example.archshowcase.core.trace.verification.NetworkRecording
import com.example.archshowcase.core.trace.verification.NetworkTape
import com.example.archshowcase.core.util.Log
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.TextContent
import kotlin.concurrent.Volatile

private const val TAG = "NetworkRecorder"

/**
 * Ktor HttpClient Plugin：录制网络请求/响应
 *
 * 仅在录制状态时录制。
 * 通过 [markStart] / [markEnd] 控制录制生命周期。
 */
object NetworkRecorder {

    @Volatile
    private var isRecording = false

    @Volatile
    private var recordings = emptyList<NetworkRecording>()

    @Volatile
    private var failedRecordings = emptyList<NetworkRecording>()

    @Volatile
    private var sequenceCounter = 0

    fun markStart() {
        sequenceCounter = 0
        recordings = emptyList()
        failedRecordings = emptyList()
        isRecording = true
        Log.d(TAG) { "Recording started" }
    }

    fun markEnd(): NetworkTape {
        isRecording = false
        val allRecordings = (recordings + failedRecordings).sortedBy { it.sequence }
        Log.d(TAG) { "Recording ended: ${recordings.size} success + ${failedRecordings.size} failed" }
        return NetworkTape(allRecordings)
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * 录制失败的请求（DNS 错误、连接超时等）
     * 不去重：HttpRequestRetry 重试 + 不同 Intent dispatch 全部记录，
     * 验证模式已禁用重试，tape 条目只按顺序消费前 N 条
     */
    fun recordFailure(method: String, url: String, error: String) {
        if (!isRecording) return
        val seq = sequenceCounter++
        failedRecordings = failedRecordings + NetworkRecording(
            sequence = seq,
            method = method,
            url = url,
            responseStatus = 0,
            responseBody = "",
            failed = true,
            errorMessage = error
        )
        Log.d(TAG) { "Recorded failure [$seq] $method $url → $error" }
    }

    val plugin = createClientPlugin("NetworkRecorder") {
        onResponse { response ->
            if (!isRecording) return@onResponse

            val call = response.call
            val seq = sequenceCounter++
            val method = call.request.method.value
            val url = call.request.url.toString()
            val statusCode = response.status.value

            try {
                val responseBody = response.bodyAsText()
                val requestBody = (call.request.content as? TextContent)?.text

                val recording = NetworkRecording(
                    sequence = seq,
                    method = method,
                    url = url,
                    requestBody = requestBody,
                    responseStatus = statusCode,
                    responseBody = responseBody
                )
                recordings = recordings + recording
                Log.d(TAG) { "Recorded [$seq] $method $url → $statusCode" }
            } catch (e: Exception) {
                Log.w(TAG) { "Failed to record response for [$seq]: ${e.message}" }
            }
        }
    }
}
