package com.example.archshowcase.chat.repository

import com.example.archshowcase.chat.mock.MockDataGenerator
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.chat.model.WindowAnchor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Preview 专用 ChatRepository — 纯内存，不依赖 SQLDelight。
 * layoutlib 不支持 AndroidSqliteDriver，所以 Preview 环境需要绕过 DB 层。
 */
class PreviewChatRepository : ChatRepository {

    private val groups = MockDataGenerator.generateGroups()
    private val groupMap = groups.associateBy { it.id }
    private val messagesMap = groups.associate { it.id to MutableStateFlow(it.messages) }

    override val conversations: Flow<List<Conversation>> = MutableStateFlow(
        groups.map { g ->
            Conversation(
                id = g.id, name = g.name,
                memberAvatars = g.members.map { it.avatar },
                memberCount = g.members.size + 1,
                lastActiveTime = g.messages.lastOrNull()?.timestamp ?: 0,
                unreadCount = 0
            )
        }
    )

    override val totalUnread: Flow<Long> = MutableStateFlow(0L)

    override fun observeMessages(
        conversationId: String,
        anchor: StateFlow<WindowAnchor>,
        windowSize: Int
    ): Flow<MessageWindow> {
        ensureGroup(conversationId)
        return (messagesMap[conversationId] ?: MutableStateFlow(emptyList())).map { messages ->
            MessageWindow(messages = messages, hasMoreBefore = false, hasMoreAfter = false, newMessageCount = 0)
        }
    }

    override suspend fun sendMessage(conversationId: String, body: MessageBody): Result<ChatMessage> {
        val msg = ChatMessage(
            "preview_msg", conversationId, MockDataGenerator.LOCAL_USER_ID,
            "Me", null, body, 0, true, SendStatus.SENT
        )
        return Result.success(msg)
    }

    override suspend fun resendMessage(messageId: String, conversationId: String) = Result.success(
        ChatMessage(messageId, conversationId, "", "", null, MessageBody.Text(""), 0, true, SendStatus.SENT)
    )

    override suspend fun loadMoreHistory(conversationId: String, beforeTimestamp: Long, beforeId: String) =
        Result.success(emptyList<ChatMessage>())

    override suspend fun recallMessage(messageId: String, conversationId: String) = Result.success(Unit)
    override suspend fun deleteMessage(messageId: String, conversationId: String) = Result.success(Unit)
    override fun markRead(conversationId: String) {}
    override fun getConversationName(conversationId: String): String? {
        ensureGroup(conversationId)
        return groupMap[conversationId]?.name
    }

    override fun getMemberCount(conversationId: String): Int {
        ensureGroup(conversationId)
        return (groupMap[conversationId]?.members?.size ?: 0) + 1
    }

    override fun observeTypingState(conversationId: String): StateFlow<Pair<Boolean, String?>> =
        MutableStateFlow(Pair(false, null))

    private fun ensureGroup(conversationId: String) {
        if (conversationId in groupMap) return
        val g = MockDataGenerator.generateSingleGroup(conversationId)
        (groupMap as MutableMap)[g.id] = g
        (messagesMap as MutableMap)[g.id] = MutableStateFlow(g.messages)
    }
}
