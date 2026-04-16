package com.example.archshowcase.presentation.chat.room

import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.chat.model.WindowAnchor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatRoomHistoryTest {

    private val baseState = ChatRoomStore.State(conversationId = "conv_1")

    // ── WindowAnchor.fromString ──

    @Test
    fun `WindowAnchor fromString parses Latest`() {
        val anchor = WindowAnchor.fromString("Latest")
        assertIs<WindowAnchor.Latest>(anchor)
    }

    @Test
    fun `WindowAnchor fromString parses At`() {
        val anchor = WindowAnchor.fromString("At:5000:msg_50")
        assertIs<WindowAnchor.At>(anchor)
        assertEquals(5000L, anchor.timestamp)
        assertEquals("msg_50", anchor.id)
    }

    @Test
    fun `WindowAnchor fromString defaults to Latest for unknown`() {
        val anchor = WindowAnchor.fromString("unknown")
        assertIs<WindowAnchor.Latest>(anchor)
    }

    // ── WindowChanged ──

    @Test
    fun `WindowChanged record serialization under 250 bytes`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.WindowChanged(
                anchorTimestamp = 1710000000000,
                anchorId = "msg_12345",
                windowSize = 200
            ),
            timestamp = 1710000001000
        )
        val json = Json.encodeToString(record)
        assertTrue(json.toByteArray().size < 250, "Serialized size=${json.toByteArray().size} should be <250 bytes")
    }

    @Test
    fun `WindowChanged applyToState with windowOverride sets messages directly`() {
        val msgs = listOf(
            ChatMessage("m1", "conv_1", "u1", "U1", null, MessageBody.Text("hi"), 2000, false, SendStatus.SENT),
            ChatMessage("m2", "conv_1", "u1", "U1", null, MessageBody.Text("yo"), 1000, false, SendStatus.SENT)
        )
        val window = MessageWindow(
            messages = msgs,
            hasMoreBefore = true,
            hasMoreAfter = false,
            newMessageCount = 0
        )
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.WindowChanged(anchorTimestamp = 2000, anchorId = "m1", windowSize = 200),
            timestamp = 100
        )

        val newState = record.applyToState(baseState, window)

        assertEquals(msgs, newState.messages)
        assertTrue(newState.hasMoreBefore)
        assertEquals(0, newState.newMessageCount)
    }

    @Test
    fun `WindowChanged applyToState without windowOverride keeps prevState messages - pure`() {
        val msgs = listOf(
            ChatMessage("m1", "conv_1", "u1", "U1", null, MessageBody.Text("hi"), 2000, false, SendStatus.SENT)
        )
        val prevState = baseState.copy(messages = msgs, hasMoreBefore = true, newMessageCount = 3)
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.WindowChanged(anchorTimestamp = 2000, anchorId = "m1", windowSize = 200),
            timestamp = 100
        )

        val newState = record.applyToState(prevState)

        assertEquals(msgs, newState.messages)
        assertTrue(newState.hasMoreBefore)
        assertEquals(3, newState.newMessageCount)
    }

    @Test
    fun `WindowChanged toIntent returns MoveWindow At`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.WindowChanged(anchorTimestamp = 5000, anchorId = "msg_50", windowSize = 200),
            timestamp = 100
        )
        val intent = record.toIntent()
        assertIs<ChatRoomStore.Intent.MoveWindow>(intent)
        val anchor = intent.anchor
        assertIs<WindowAnchor.At>(anchor)
        assertEquals(5000L, anchor.timestamp)
        assertEquals("msg_50", anchor.id)
    }

    @Test
    fun `WindowChanged toIntent with empty anchorId returns MoveWindow Latest`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.WindowChanged(anchorTimestamp = 0, anchorId = "", windowSize = 200),
            timestamp = 100
        )
        val intent = record.toIntent()
        assertIs<ChatRoomStore.Intent.MoveWindow>(intent)
        assertIs<WindowAnchor.Latest>(intent.anchor)
    }

    // ── Initialized ──

    @Test
    fun `Initialized applyToState sets conversation fields`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.Initialized("conv_1", "Group Chat", 10),
            timestamp = 100
        )

        val newState = record.applyToState(ChatRoomStore.State())

        assertEquals("conv_1", newState.conversationId)
        assertEquals("Group Chat", newState.conversationName)
        assertEquals(10, newState.memberCount)
    }

    @Test
    fun `Initialized toIntent returns Init`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.Initialized("conv_1", "Group Chat", 10),
            timestamp = 100
        )
        val intent = record.toIntent()
        assertIs<ChatRoomStore.Intent.Init>(intent)
        assertEquals("conv_1", intent.conversationId)
    }

    // ── Scroll ──

    @Test
    fun `Scroll applyToState sets scrollPosition`() {
        val position = ScrollPosition(firstVisibleIndex = 5, offset = 120)
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.Scroll(position),
            timestamp = 100
        )

        val newState = record.applyToState(baseState)

        assertEquals(5, newState.scrollPosition.firstVisibleIndex)
        assertEquals(120, newState.scrollPosition.offset)
    }

    @Test
    fun `Scroll toIntent returns UpdateScrollPosition`() {
        val position = ScrollPosition(firstVisibleIndex = 5, offset = 120)
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.Scroll(position),
            timestamp = 100
        )
        val intent = record.toIntent()
        assertIs<ChatRoomStore.Intent.UpdateScrollPosition>(intent)
        assertEquals(5, intent.firstVisibleIndex)
        assertEquals(120, intent.offset)
    }

    // ── ToggleInputMode ──

    @Test
    fun `ToggleInputMode applyToState sets inputMode`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.ToggleInputMode(InputMode.VOICE),
            timestamp = 100
        )

        val newState = record.applyToState(baseState)

        assertEquals(InputMode.VOICE, newState.inputMode)
    }

    @Test
    fun `ToggleInputMode toIntent returns ToggleInputMode`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.ToggleInputMode(InputMode.VOICE),
            timestamp = 100
        )
        assertIs<ChatRoomStore.Intent.ToggleInputMode>(record.toIntent())
    }

    // ── ToggleEmojiPanel ──

    @Test
    fun `ToggleEmojiPanel applyToState opens emoji and closes plus`() {
        val prevState = baseState.copy(showPlusPanel = true)
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.ToggleEmojiPanel(true),
            timestamp = 100
        )

        val newState = record.applyToState(prevState)

        assertTrue(newState.showEmojiPanel)
        assertFalse(newState.showPlusPanel)
    }

    @Test
    fun `ToggleEmojiPanel toIntent returns ToggleEmojiPanel`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.ToggleEmojiPanel(true),
            timestamp = 100
        )
        assertIs<ChatRoomStore.Intent.ToggleEmojiPanel>(record.toIntent())
    }

    // ── TogglePlusPanel ──

    @Test
    fun `TogglePlusPanel applyToState opens plus and closes emoji`() {
        val prevState = baseState.copy(showEmojiPanel = true)
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.TogglePlusPanel(true),
            timestamp = 100
        )

        val newState = record.applyToState(prevState)

        assertTrue(newState.showPlusPanel)
        assertFalse(newState.showEmojiPanel)
    }

    @Test
    fun `TogglePlusPanel toIntent returns TogglePlusPanel`() {
        val record = ChatRoomHistoryRecord(
            type = ChatRoomHistoryType.TogglePlusPanel(true),
            timestamp = 100
        )
        assertIs<ChatRoomStore.Intent.TogglePlusPanel>(record.toIntent())
    }

}
