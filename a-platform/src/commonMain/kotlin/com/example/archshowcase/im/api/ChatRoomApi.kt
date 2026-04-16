package com.example.archshowcase.im.api

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.model.SendMessageRequest
import com.example.archshowcase.network.ApiRoutes
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ChatRoomApi {
    suspend fun sendMessage(request: SendMessageRequest): Result<Unit>
}

class DefaultChatRoomApi : ChatRoomApi, KoinComponent {

    private val client: HttpClient by inject()

    override suspend fun sendMessage(request: SendMessageRequest): Result<Unit> = runCatching {
        Log.d(TAG) { "Sending message to room: ${request.roomId}" }
        client.post(ApiRoutes.ROOMS_SEND_MSG) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        Unit
    }.onFailure { e ->
        Log.e(TAG, e) { "Failed to send message to room: ${request.roomId}" }
    }

    companion object {
        private const val TAG = "ChatRoomApi"
    }
}

class MockChatRoomApi : ChatRoomApi {
    override suspend fun sendMessage(request: SendMessageRequest): Result<Unit> {
        Log.d(TAG) { "Mock send message to room: ${request.roomId}" }
        return Result.success(Unit)
    }

    companion object {
        private const val TAG = "MockChatRoomApi"
    }
}
