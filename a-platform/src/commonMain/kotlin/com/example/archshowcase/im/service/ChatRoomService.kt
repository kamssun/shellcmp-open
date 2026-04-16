package com.example.archshowcase.im.service

import com.example.archshowcase.im.model.HistoryQuery
import com.example.archshowcase.im.model.ImMessage
import com.example.archshowcase.im.model.RoomStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRoomService {
    val roomStatusFlow: StateFlow<RoomStatus>
    val messagesFlow: Flow<ImMessage>

    fun enterRoom(
        roomId: String,
        onSuccess: (roomId: String) -> Unit = {},
        onError: (code: Int) -> Unit = {}
    )

    fun leaveRoom(roomId: String)

    suspend fun sendMessage(content: String, msgType: Int = MSG_TYPE_TEXT): Result<Unit>

    suspend fun pullHistory(query: HistoryQuery): Result<List<ImMessage>>

    companion object {
        const val MSG_TYPE_TEXT = 1
    }
}
