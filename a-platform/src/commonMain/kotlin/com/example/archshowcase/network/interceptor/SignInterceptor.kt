package com.example.archshowcase.network.interceptor

import com.example.archshowcase.devicetoken.DeviceTokenService
import com.example.archshowcase.network.header.HeaderConstants
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders

/**
 * 签名拦截器
 *
 * 在所有 header（含 Auth Bearer）就绪后，基于指定字段计算签名并追加到请求。
 */
suspend fun interceptSign(request: HttpRequestBuilder, deviceTokenService: DeviceTokenService) {
    val fields = buildMap {
        put("ApiPath", request.url.build().encodedPath)
        put("Apikey", request.headers[HeaderConstants.API_KEY].orEmpty())
        put("Codetag", request.headers[HeaderConstants.CODE_TAG].orEmpty())
        put("DeviceId", request.headers[HeaderConstants.DEVICE_ID].orEmpty())
        put("LoginToken", request.headers[HttpHeaders.Authorization].orEmpty())
        put("MlKeyExt", request.headers[HeaderConstants.ML_KEY_EXT].orEmpty())
        put("Noncestr", request.headers[HeaderConstants.NONCESTR].orEmpty())
        put("Timestamp", request.headers[HeaderConstants.TIMESTAMP].orEmpty())
        put("Language", request.headers[HeaderConstants.LANGUAGE].orEmpty())
    }
    deviceTokenService.sign(fields)?.let { signToken ->
        request.headers.append(HeaderConstants.SIGN_TOKEN, signToken)
    }
}
