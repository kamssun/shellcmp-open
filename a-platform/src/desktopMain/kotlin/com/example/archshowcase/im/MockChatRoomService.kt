package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.model.HistoryQuery
import com.example.archshowcase.im.model.ImMessage
import com.example.archshowcase.im.model.MessageType
import com.example.archshowcase.im.model.RoomStatus
import com.example.archshowcase.im.service.ChatRoomService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class MockChatRoomService : ChatRoomService {

    private val _roomStatusFlow = MutableStateFlow<RoomStatus>(RoomStatus.Idle)
    override val roomStatusFlow: StateFlow<RoomStatus> = _roomStatusFlow.asStateFlow()

    private val _messagesFlow = MutableSharedFlow<ImMessage>(extraBufferCapacity = 64)
    override val messagesFlow: Flow<ImMessage> = _messagesFlow.asSharedFlow()

    private var currentRoomId: String? = null

    override fun enterRoom(roomId: String, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        currentRoomId = roomId
        Log.d(TAG) { "Mock enter room: $roomId" }
        _roomStatusFlow.value = RoomStatus.Joined(roomId)
        onSuccess(roomId)

        _messagesFlow.tryEmit(
            ImMessage(
                id = "mock_welcome",
                content = "Welcome to mock room $roomId",
                senderId = "system",
                roomId = roomId,
                timestamp = System.currentTimeMillis(),
                type = MessageType.SYSTEM
            )
        )
    }

    override suspend fun sendMessage(content: String, msgType: Int): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("Not in a room"))
        Log.d(TAG) { "Mock send message to room: $roomId" }
        _messagesFlow.tryEmit(
            ImMessage(
                id = "mock_echo_${System.currentTimeMillis()}",
                content = content,
                senderId = "local_user",
                roomId = roomId,
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT
            )
        )
        return Result.success(Unit)
    }

    override suspend fun pullHistory(query: HistoryQuery): Result<List<ImMessage>> {
        Log.d(TAG) { "Mock pullHistory: room=${query.roomId}, limit=${query.limit}" }
        return Result.success(emptyList())
    }

    override fun leaveRoom(roomId: String) {
        Log.d(TAG) { "Mock leave room: $roomId" }
        currentRoomId = null
        _roomStatusFlow.value = RoomStatus.Idle
    }

    companion object {
        private const val TAG = "MockChatRoomService"
    }
}
