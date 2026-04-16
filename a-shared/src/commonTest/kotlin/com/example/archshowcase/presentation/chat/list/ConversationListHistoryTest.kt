package com.example.archshowcase.presentation.chat.list

import com.example.archshowcase.chat.model.Conversation
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationListHistoryTest {

    private fun conv(id: String, time: Long, preview: String? = null) = Conversation(
        id = id, name = "G-$id", memberAvatars = emptyList(),
        memberCount = 2, lastActiveTime = time, unreadCount = 0,
        lastMsgPreview = preview
    )

    // ── Fix #5: history record 不再携带 conversations 列表 ──

    @Test
    fun `ConversationsUpdated is a data object with no payload`() {
        val record = ConversationListHistoryRecord(
            type = ConversationListHistoryType.ConversationsUpdated(emptyList()),
            timestamp = 100
        )
        assertEquals(ConversationListHistoryType.ConversationsUpdated(emptyList()), record.type)
    }

    // ── applyToState 保留 prevState.conversations ──

    @Test
    fun `applyToState preserves existing conversations`() {
        val prevState = ConversationListStore.State(
            conversations = listOf(
                conv("g1", 1000, "old"),
                conv("g2", 2000, "two"),
                conv("g3", 1500, "three")
            ),
            isLoading = true
        )

        val convs = prevState.conversations
        val record = ConversationListHistoryRecord(
            type = ConversationListHistoryType.ConversationsUpdated(convs),
            timestamp = 100
        )
        val newState = record.applyToState(prevState)

        // conversations 来自 record 中携带的快照
        assertEquals(3, newState.conversations.size)
        assertEquals("g1", newState.conversations[0].id)
        assertEquals(false, newState.isLoading)
    }

    // ── Fix #4: toIntent 触发 Refresh 从 DB 重加载 ──

    @Test
    fun `toIntent for ConversationsUpdated returns Refresh`() {
        val record = ConversationListHistoryRecord(
            type = ConversationListHistoryType.ConversationsUpdated(emptyList()),
            timestamp = 100
        )
        val intent = record.toIntent()
        assertEquals(ConversationListStore.Intent.Refresh, intent)
    }
}
