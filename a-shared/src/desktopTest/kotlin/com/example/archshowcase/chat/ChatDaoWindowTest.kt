@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.archshowcase.chat

import com.example.archshowcase.chat.db.ChatDatabase
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.chat.local.ChatDao
import com.example.archshowcase.chat.local.DatabaseDriverFactory
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.chat.model.WindowAnchor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatDaoWindowTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dao: ChatDao by inject()

    private val convId = "test_conv"

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single { ChatDatabase(DatabaseDriverFactory().create()) }
                singleOf(::ChatDao)
            })
        }
        AppConfig.useOBOScheduler = false
        Dispatchers.setMain(testDispatcher)
        seedMessages()
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        stopKoin()
    }

    private fun makeMsg(id: String, ts: Long) = ChatMessage(
        id = id, conversationId = convId, senderId = "u1", senderName = "User1",
        senderAvatar = null, body = MessageBody.Text("msg $id"),
        timestamp = ts, isMine = false, status = SendStatus.SENT
    )

    private fun seedMessages() {
        dao.insertConversation(convId, "Test", 2, 0)
        // Insert 50 messages: msg_01..msg_50 with timestamps 1000..50000
        val msgs = (1..50).map { i -> makeMsg("msg_${i.toString().padStart(2, '0')}", i * 1000L) }
        dao.insertMessages(msgs)
    }

    // ── Task 3.1: Latest 模式返回最新 windowSize 条消息 ──

    @Test
    fun `Latest mode returns newest messages`() = runTest {
        val anchor = MutableStateFlow<WindowAnchor>(WindowAnchor.Latest)
        val window = dao.observeWindow(convId, anchor, windowSize = 10).first()

        assertEquals(10, window.messages.size)
        // 最新消息在前（timestamp DESC）
        assertEquals("msg_50", window.messages.first().id)
        assertEquals("msg_41", window.messages.last().id)
        assertTrue(window.hasMoreBefore)
        assertFalse(window.hasMoreAfter)
        assertEquals(0, window.newMessageCount)
    }

    // ── Task 3.2: At 模式返回 anchor 前后各 halfWindow 条消息 ──

    @Test
    fun `At mode returns messages around anchor`() = runTest {
        val anchor = MutableStateFlow<WindowAnchor>(WindowAnchor.At(25000, "msg_25"))
        val window = dao.observeWindow(convId, anchor, windowSize = 10).first()

        // halfWindow=5, anchor included in after part
        assertTrue(window.messages.any { it.id == "msg_25" })
        assertTrue(window.hasMoreBefore)
        assertTrue(window.hasMoreAfter)
    }

    // ── Task 3.3: anchor 变更触发 re-emit（从 observeWindow flow 收集） ──

    @Test
    fun `anchor change triggers re-emit`() = runTest {
        val anchor = MutableStateFlow<WindowAnchor>(WindowAnchor.Latest)
        val emissions = mutableListOf<MessageWindow>()
        val job = launch(Dispatchers.Default) {
            dao.observeWindow(convId, anchor, windowSize = 10).collect { emissions.add(it) }
        }

        // 等待首次 emission（flow 在 Dispatchers.Default 上 sample(100)）
        withContext(Dispatchers.Default) { delay(500) }
        assertEquals(1, emissions.size, "Expected 1st emission")
        assertEquals("msg_50", emissions[0].messages.first().id)

        // 变更 anchor → 触发 re-emit
        anchor.value = WindowAnchor.At(10000, "msg_10")
        withContext(Dispatchers.Default) { delay(500) }
        assertTrue(emissions.size >= 2, "Expected re-emit after anchor change, got ${emissions.size}")
        assertTrue(emissions.last().messages.any { it.id == "msg_10" })

        job.cancel()
    }

    // ── Task 3.4: messageChange 触发 re-emit（从 observeWindow flow 收集） ──

    @Test
    fun `messageChange triggers re-emit`() = runTest {
        val anchor = MutableStateFlow<WindowAnchor>(WindowAnchor.Latest)
        val emissions = mutableListOf<MessageWindow>()
        val job = launch(Dispatchers.Default) {
            dao.observeWindow(convId, anchor, windowSize = 60).collect { emissions.add(it) }
        }

        // 等待首次 emission
        withContext(Dispatchers.Default) { delay(500) }
        assertEquals(1, emissions.size, "Expected 1st emission")
        assertEquals(50, emissions[0].messages.size)

        // 插入新消息 → 触发 messageChange → re-emit
        dao.insertMessage(makeMsg("msg_new", 99000))
        withContext(Dispatchers.Default) { delay(500) }
        assertTrue(emissions.size >= 2, "Expected re-emit after insert, got ${emissions.size}")
        assertEquals(51, emissions.last().messages.size)
        assertEquals("msg_new", emissions.last().messages.first().id)

        job.cancel()
    }

    // ── Task 3.5: newMessageCount 在 At 模式下正确统计 ──

    @Test
    fun `newMessageCount counts all messages after newestSeen`() {
        // anchor at msg_40 (ts=40000): msg_41..msg_50 = 10 new messages
        // newestSeen 未设置 → 退化为 anchor 位置 → newCount = 10
        val window = dao.queryWindowSync(convId, WindowAnchor.At(40000, "msg_40"), 10)
        assertEquals(10, window.newMessageCount)
    }

    // ── Task 3.6: hasMoreBefore / hasMoreAfter 边界正确 ──

    @Test
    fun `hasMoreBefore false when at oldest end`() {
        val window = dao.queryWindowSync(convId, WindowAnchor.At(1000, "msg_01"), 100)
        assertFalse(window.hasMoreBefore)
    }

    @Test
    fun `hasMoreAfter false in Latest mode`() {
        val window = dao.queryWindowSync(convId, WindowAnchor.Latest, 10)
        assertFalse(window.hasMoreAfter)
    }

    // ── 连续滑窗收敛：从 Latest 向旧端滑到底 ──

    @Test
    fun `sliding towards oldest eventually reaches beginning`() {
        var anchor: WindowAnchor = WindowAnchor.Latest
        var window = dao.queryWindowSync(convId, anchor, 10)
        var iterations = 0

        while (window.hasMoreBefore && iterations < 20) {
            val oldest = window.messages.last()
            anchor = WindowAnchor.At(oldest.timestamp, oldest.id)
            window = dao.queryWindowSync(convId, anchor, 10)
            iterations++
        }

        assertFalse(window.hasMoreBefore)
        assertTrue(window.messages.any { it.id == "msg_01" })
    }

    // ── 相邻窗口无间隙：边界处有重叠消息 ──

    @Test
    fun `adjacent windows overlap at boundary`() {
        val w1 = dao.queryWindowSync(convId, WindowAnchor.Latest, 10)
        val w1Oldest = w1.messages.last()

        val w2 = dao.queryWindowSync(convId, WindowAnchor.At(w1Oldest.timestamp, w1Oldest.id), 10)

        assertTrue(w2.messages.any { it.id == w1Oldest.id },
            "Adjacent windows should overlap at boundary")
        assertTrue(w2.messages.minOf { it.timestamp } < w1.messages.minOf { it.timestamp },
            "Second window should reach further back")
    }
}
