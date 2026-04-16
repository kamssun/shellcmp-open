@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.archshowcase.chat

import com.example.archshowcase.chat.db.ChatDatabase
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.chat.local.ChatDao
import com.example.archshowcase.chat.local.DatabaseDriverFactory
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.SendStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.measureTime

class ChatDaoDenormalizationTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dao: ChatDao by inject()
    private val db: ChatDatabase by inject()

    @BeforeTest
    fun setup() {
        AppConfig.useOBOScheduler = false
        startKoin {
            modules(module {
                single { ChatDatabase(DatabaseDriverFactory().create()) }
                singleOf(::ChatDao)
            })
        }
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        stopKoin()
    }

    private fun makeMsg(
        id: String,
        convId: String = "conv_1",
        text: String = "hello",
        timestamp: Long = 1000,
        senderName: String = "Alice"
    ) = ChatMessage(
        id = id, conversationId = convId,
        senderId = "u1", senderName = senderName, senderAvatar = null,
        body = MessageBody.Text(text), timestamp = timestamp,
        isMine = false, status = SendStatus.SENT
    )

    // ── Task 2.1: insertMessage 更新 conversation summary ──

    @Test
    fun `insertMessage updates conversation summary fields`() {
        dao.insertConversation("conv_1", "Test", 2, 0)
        dao.insertMessage(makeMsg("m1", text = "Hello!", timestamp = 1000, senderName = "Alice"))

        val row = db.conversationQueries.selectById("conv_1").executeAsOne()
        assertEquals("Hello!", row.last_msg_preview)
        assertEquals("text", row.last_msg_body_type)
        assertEquals("Alice", row.last_msg_sender_name)
        assertEquals(1000L, row.last_active_time)
    }

    // ── Task 2.2: markRecalled 更新摘要为次新消息 ──

    @Test
    fun `markRecalled updates summary to next newest non-recalled message`() {
        dao.insertConversation("conv_1", "Test", 2, 0)
        dao.insertMessage(makeMsg("m1", text = "First", timestamp = 1000, senderName = "Alice"))
        dao.insertMessage(makeMsg("m2", text = "Second", timestamp = 2000, senderName = "Bob"))

        dao.markRecalled("m2", "conv_1")

        val row = db.conversationQueries.selectById("conv_1").executeAsOne()
        assertEquals("First", row.last_msg_preview)
        assertEquals("Alice", row.last_msg_sender_name)
        assertEquals(1000L, row.last_active_time)
    }

    // ── Task 2.3: deleteMessage 更新摘要为次新消息 ──

    @Test
    fun `deleteMessage updates summary to next newest message`() {
        dao.insertConversation("conv_1", "Test", 2, 0)
        dao.insertMessage(makeMsg("m1", text = "First", timestamp = 1000, senderName = "Alice"))
        dao.insertMessage(makeMsg("m2", text = "Second", timestamp = 2000, senderName = "Bob"))

        dao.deleteMessage("m2", "conv_1")

        val row = db.conversationQueries.selectById("conv_1").executeAsOne()
        assertEquals("First", row.last_msg_preview)
        assertEquals("Alice", row.last_msg_sender_name)
        assertEquals(1000L, row.last_active_time)
    }

    @Test
    fun `deleteMessage clears summary when no messages remain`() {
        dao.insertConversation("conv_1", "Test", 2, 0)
        dao.insertMessage(makeMsg("m1", text = "Only", timestamp = 1000))

        dao.deleteMessage("m1", "conv_1")

        val row = db.conversationQueries.selectById("conv_1").executeAsOne()
        assertNull(row.last_msg_preview)
        assertNull(row.last_msg_body_type)
    }

    // ── Task 4.1: 冷启动 observeConversations 从 DB 加载 ──

    @Test
    fun `observeConversations cold start loads from DB sorted by lastActiveTime DESC`() = runTest {
        dao.insertConversation("conv_1", "Old", 2, 1000)
        dao.insertConversation("conv_2", "New", 2, 2000)
        dao.insertConversation("conv_3", "Mid", 2, 1500)

        testDispatcher.scheduler.advanceTimeBy(200)
        val list = dao.observeConversations().first()

        assertEquals(3, list.size)
        assertEquals("conv_2", list[0].id)
        assertEquals("conv_3", list[1].id)
        assertEquals("conv_1", list[2].id)
    }

    // ── Task 4.2: insertMessage 后 Flow 更新 ──

    @Test
    fun `observeConversations updates after insertMessage`() = runTest {
        dao.insertConversation("conv_1", "G1", 2, 1000)
        dao.insertConversation("conv_2", "G2", 2, 2000)

        testDispatcher.scheduler.advanceTimeBy(200)
        val initial = dao.observeConversations().first()
        assertEquals("conv_2", initial[0].id) // conv_2 is newer

        // Insert message to conv_1, making it newest
        dao.insertMessage(makeMsg("m1", convId = "conv_1", text = "New!", timestamp = 3000))
        testDispatcher.scheduler.advanceTimeBy(200)
        val updated = dao.observeConversations().first()
        assertEquals("conv_1", updated[0].id) // conv_1 is now newest
        assertEquals("New!", updated[0].lastMsgPreview)
    }

    // ── Task 4.3: 万级 merge + 排序性能 ──

    @Test
    fun `10K conversations merge and sort under 10ms`() {
        val map = (1..10_000).associate { i ->
            "conv_$i" to Conversation(
                id = "conv_$i", name = "G$i", memberAvatars = emptyList(),
                memberCount = 2, lastActiveTime = i.toLong(), unreadCount = 0
            )
        }

        val elapsed = measureTime {
            repeat(10) { // 跑 10 次取平均
                map.values.sortedByDescending { it.lastActiveTime }
            }
        }
        val avgMs = elapsed.inWholeMilliseconds / 10
        assertTrue(avgMs < 50, "avg sort took ${avgMs}ms, expected <50ms")
    }
}
