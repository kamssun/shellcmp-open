package com.example.archshowcase.core.trace.verification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 单条网络请求/响应录制
 *
 * 支持成功响应和失败请求（DNS 错误、连接超时等）：
 * - 成功：responseStatus > 0, responseBody 有内容
 * - 失败：failed = true, errorMessage 保存原始错误信息
 */
@Serializable
data class NetworkRecording(
    val sequence: Int,
    val method: String,
    val url: String,
    @SerialName("request_body") val requestBody: String? = null,
    @SerialName("response_status") val responseStatus: Int,
    @SerialName("response_body") val responseBody: String,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val failed: Boolean = false,
    @SerialName("error_message") val errorMessage: String? = null
)

/**
 * 网络录制集合（tape）
 */
@Serializable
data class NetworkTape(
    val recordings: List<NetworkRecording>
)
