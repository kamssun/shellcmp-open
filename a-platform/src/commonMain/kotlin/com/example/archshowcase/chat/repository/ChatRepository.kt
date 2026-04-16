package com.example.archshowcase.chat.repository

import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.WindowAnchor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {

    val conversations: Flow<List<Conversation>>
    val totalUnread: Flow<Long>

    fun observeMessages(
        conversationId: String,
        anchor: StateFlow<WindowAnchor>,
        windowSize: Int = MessageWindow.DEFAULT_WINDOW_SIZE
    ): Flow<MessageWindow>

    suspend fun sendMessage(conversationId: String, body: MessageBody): Result<ChatMessage>

    suspend fun resendMessage(messageId: String, conversationId: String): Result<ChatMessage>

    suspend fun loadMoreHistory(conversationId: String, beforeTimestamp: Long, beforeId: String): Result<List<ChatMessage>>

    suspend fun recallMessage(messageId: String, conversationId: String): Result<Unit>

    suspend fun deleteMessage(messageId: String, conversationId: String): Result<Unit>

    fun markRead(conversationId: String)

    fun getConversationName(conversationId: String): String?

    fun getMemberCount(conversationId: String): Int

    fun observeTypingState(conversationId: String): StateFlow<Pair<Boolean, String?>>

    fun onLeaveConversation() {}
}
