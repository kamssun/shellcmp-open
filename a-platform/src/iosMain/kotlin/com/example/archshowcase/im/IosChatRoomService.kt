package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.api.ChatRoomApi
import com.example.archshowcase.im.model.HistoryQuery
import com.example.archshowcase.im.model.ImMessage
import com.example.archshowcase.im.model.MessageType
import com.example.archshowcase.im.model.RoomStatus
import com.example.archshowcase.im.model.SendMessageRequest
import com.example.archshowcase.im.service.ChatRoomService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.example.archshowcase.getImBridgeOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.coroutines.resume

class IosChatRoomService : ChatRoomService, KoinComponent {

    private val chatRoomApi: ChatRoomApi by inject()
    private val _roomStatusFlow = MutableStateFlow<RoomStatus>(RoomStatus.Idle)
    override val roomStatusFlow: StateFlow<RoomStatus> = _roomStatusFlow.asStateFlow()

    private val _messagesFlow = MutableSharedFlow<ImMessage>(extraBufferCapacity = 64)
    override val messagesFlow: Flow<ImMessage> = _messagesFlow.asSharedFlow()

    private var currentRoomId: String? = null

    private val messageCallback = object : ImMessageCallback {
        override fun onMessage(
            id: String,
            content: String,
            senderId: String,
            roomId: String?,
            timestamp: Long,
            typeOrdinal: Int,
            rawJson: String?
        ) {
            val message = ImMessage(
                id = id,
                content = content,
                senderId = senderId,
                roomId = roomId,
                timestamp = timestamp,
                type = MessageType.entries.getOrElse(typeOrdinal) { MessageType.CUSTOM },
                rawJson = rawJson
            )
            _messagesFlow.tryEmit(message)
        }
    }

    private val roomStatusCallback = object : ImRoomStatusCallback {
        override fun onRoomStatusChanged(statusType: String, roomId: String?, message: String?) {
            val newStatus = when (statusType) {
                "joining" -> RoomStatus.Joining
                "joined" -> RoomStatus.Joined(roomId.orEmpty())
                "error" -> RoomStatus.Error(message ?: "unknown")
                "kicked" -> RoomStatus.KickedOut(roomId.orEmpty(), message ?: "unknown")
                "idle" -> RoomStatus.Idle
                else -> RoomStatus.Idle
            }
            _roomStatusFlow.value = newStatus
        }
    }

    override fun enterRoom(roomId: String, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        val bridge = getImBridgeOrNull()
        if (bridge == null) {
            _roomStatusFlow.value = RoomStatus.Error("ImBridge not set")
            onError(-1)
            return
        }

        currentRoomId?.takeIf { it != roomId }?.let { leaveRoom(it) }

        _roomStatusFlow.value = RoomStatus.Joining
        currentRoomId = roomId

        bridge.setMessageCallback(messageCallback)
        bridge.setRoomStatusCallback(roomStatusCallback)

        bridge.enterRoom(roomId, object : ImBridgeCallback {
            override fun onSuccess(message: String) {
                Log.d(TAG) { "Enter room success: $roomId" }
                _roomStatusFlow.value = RoomStatus.Joined(roomId)
                onSuccess(roomId)
            }

            override fun onError(code: Int, message: String) {
                Log.e(TAG) { "Enter room failed: code=$code, msg=$message" }
                _roomStatusFlow.value = RoomStatus.Error("enter_failed: $code")
                currentRoomId = null
                onError(code)
            }
        })
    }

    override suspend fun sendMessage(content: String, msgType: Int): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("Not in a room"))
        return chatRoomApi.sendMessage(SendMessageRequest(msgType, roomId, content))
    }

    override suspend fun pullHistory(query: HistoryQuery): Result<List<ImMessage>> =
        suspendCancellableCoroutine { cont ->
            val bridge = getImBridgeOrNull()
            if (bridge == null) {
                cont.resume(Result.failure(IllegalStateException("ImBridge not set")))
                return@suspendCancellableCoroutine
            }
            bridge.pullHistory(query.roomId, query.seqIdGte, query.seqIdLte, object : ImHistoryCallback {
                override fun onMessages(messagesJson: String) {
                    val messages = parseHistoryJson(messagesJson, query.roomId)
                    cont.resume(Result.success(messages))
                }

                override fun onError(code: Int, message: String) {
                    Log.e(TAG) { "pullHistory failed: code=$code, msg=$message" }
                    cont.resume(Result.failure(RuntimeException("pullHistory failed: $code")))
                }
            })
        }

    private fun parseHistoryJson(json: String, roomId: String): List<ImMessage> =
        runCatching {
            val array = Json.parseToJsonElement(json).jsonArray
            array.map { element ->
                val obj = element.jsonObject
                ImMessage(
                    id = obj["id"]?.jsonPrimitive?.content.orEmpty(),
                    content = obj["content"]?.jsonPrimitive?.content.orEmpty(),
                    senderId = obj["senderId"]?.jsonPrimitive?.content.orEmpty(),
                    roomId = obj["roomId"]?.jsonPrimitive?.content ?: roomId,
                    timestamp = obj["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
                    type = when (obj["type"]?.jsonPrimitive?.content) {
                        "TEXT" -> MessageType.TEXT
                        "CUSTOM" -> MessageType.CUSTOM
                        else -> MessageType.SYSTEM
                    },
                    rawJson = obj["rawJson"]?.jsonPrimitive?.content
                )
            }
        }.getOrElse {
            Log.e(TAG) { "Failed to parse history JSON: ${it.message}" }
            emptyList()
        }

    override fun leaveRoom(roomId: String) {
        val bridge = getImBridgeOrNull()
        bridge?.setMessageCallback(null)
        bridge?.setRoomStatusCallback(null)
        bridge?.leaveRoom(roomId)
        if (roomId == currentRoomId) {
            currentRoomId = null
            _roomStatusFlow.value = RoomStatus.Idle
        }
    }

    companion object {
        private const val TAG = "IosChatRoomService"
    }
}
