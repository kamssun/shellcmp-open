// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.api.ChatRoomApi
import com.example.archshowcase.im.model.HistoryQuery
import com.example.archshowcase.im.model.ImMessage
import com.example.archshowcase.im.model.RoomStatus
import com.example.archshowcase.im.model.SendMessageRequest
import com.example.archshowcase.im.service.ChatRoomService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidChatRoomService : ChatRoomService, KoinComponent {

    private val chatRoomApi: ChatRoomApi by inject()
    private val _roomStatusFlow = MutableStateFlow<RoomStatus>(RoomStatus.Idle)
    override val roomStatusFlow: StateFlow<RoomStatus> = _roomStatusFlow.asStateFlow()

    private val _messagesFlow = MutableSharedFlow<ImMessage>(extraBufferCapacity = 64)
    override val messagesFlow: Flow<ImMessage> = _messagesFlow.asSharedFlow()

    private var currentRoomId: String? = null

    override fun enterRoom(roomId: String, onSuccess: (String) -> Unit, onError: (Int) -> Unit) {
        currentRoomId?.takeIf { it != roomId }?.let { leaveRoom(it) }

        _roomStatusFlow.value = RoomStatus.Joining
        currentRoomId = roomId

        // Stub: simulate successful room join
        Log.d(TAG) { "Stub: entering room $roomId" }
        _roomStatusFlow.value = RoomStatus.Joined(roomId)
        onSuccess(roomId)
    }

    override suspend fun sendMessage(content: String, msgType: Int): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("Not in a room"))
        return chatRoomApi.sendMessage(SendMessageRequest(msgType, roomId, content))
    }

    override suspend fun pullHistory(query: HistoryQuery): Result<List<ImMessage>> {
        // Stub: no history available
        return Result.success(emptyList())
    }

    override fun leaveRoom(roomId: String) {
        if (roomId == currentRoomId) {
            currentRoomId = null
            _roomStatusFlow.value = RoomStatus.Idle
        }
    }

    companion object {
        private const val TAG = "AndroidChatRoomService"
    }
}
