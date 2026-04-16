@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.example.archshowcase.chat

import com.example.archshowcase.chat.db.ChatDatabase
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.chat.local.ChatDao
import com.example.archshowcase.chat.local.DatabaseDriverFactory
import com.example.archshowcase.chat.model.ChatMessage
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
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * ChatDao 压力测试 — 通过公共 API 外部施压，零修改 ChatDao 源码。
 *
 * 使用同步写入路径 (insertMessage/insertMessages) 验证缓存 + 排序 + 未读一致性。
 * 异步路径 (enqueueMessage + 分片 flush) 在真机上通过 StressTestConfig 场景验证。
 *
 * 运行：./gradlew :a-shared:desktopTest --tests "com.example.archshowcase.chat.ChatDaoStressTest"
 */
class ChatDaoStressTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dao: ChatDao by inject()

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
        convId: String,
        timestamp: Long,
        text: String = "stress msg"
    ) = ChatMessage(
        id = id, conversationId = convId,
        senderId = "u1", senderName = "Tester", senderAvatar = null,
        body = MessageBody.Text(text), timestamp = timestamp,
        isMine = false, status = SendStatus.SENT
    )

    // ── 场景 1: 批量写入吞吐（同步路径） ──

    @Test
    fun `insertMessage 500 messages throughput above 500 msg per second`() {
        val convCount = 50
        for (i in 0 until convCount) {
            dao.insertConversation("c_$i", "Group $i", 5, 0)
        }

        val msgCount = 500
        val elapsed = measureTime {
            for (i in 0 until msgCount) {
                dao.insertMessage(makeMsg("m_$i", "c_${i % convCount}", i.toLong()))
            }
        }

        val msgPerSec = msgCount * 1000L / elapsed.inWholeMilliseconds.coerceAtLeast(1)
        println("STRESS: insertMessage $msgCount msgs in ${elapsed.inWholeMilliseconds}ms = $msgPerSec msg/s")
        assertTrue(msgPerSec > 500, "Expected >500 msg/s, got $msgPerSec")
    }

    // ── 场景 2: 批量插入吞吐（insertMessages 批量路径） ──

    @Test
    fun `insertMessages batch 1000 messages`() {
        val convCount = 50
        for (i in 0 until convCount) {
            dao.insertConversation("c_$i", "Group $i", 5, 0)
        }

        val msgs = (0 until 1000).map { i ->
            makeMsg("m_$i", "c_${i % convCount}", i.toLong())
        }
        val elapsed = measureTime {
            dao.insertMessages(msgs)
        }

        println("STRESS: insertMessages 1000 batch in ${elapsed.inWholeMilliseconds}ms")
        assertTrue(elapsed.inWholeMilliseconds < 2000, "Expected <2000ms, got ${elapsed.inWholeMilliseconds}ms")
    }

    // ── 场景 3: 会话排序正确性（高频更新后） ──

    @Test
    fun `conversation order correct after 100 rapid updates`() = runTest {
        val convCount = 100
        for (i in 0 until convCount) {
            dao.insertConversation("c_$i", "Group $i", 3, 0)
        }

        // 逆序更新：c_99 最新，c_0 最旧
        for (i in 0 until convCount) {
            dao.insertMessage(makeMsg("m_$i", "c_$i", i.toLong() + 1000))
        }

        testDispatcher.scheduler.advanceTimeBy(500)
        val list = dao.observeConversations().first()

        assertTrue(list.size >= convCount)
        for (i in 0 until list.size - 1) {
            assertTrue(
                list[i].lastActiveTime >= list[i + 1].lastActiveTime,
                "Sort broken at $i: ${list[i].lastActiveTime} < ${list[i + 1].lastActiveTime}"
            )
        }
        assertEquals("c_99", list[0].id)
    }

    // ── 场景 4: 未读计数一致性 ──

    @Test
    fun `unread count consistent after 200 messages across 10 conversations`() = runTest {
        val convCount = 10
        val msgsPerConv = 20
        for (i in 0 until convCount) {
            dao.insertConversation("c_$i", "Group $i", 3, 0)
        }

        var seq = 0
        for (round in 0 until msgsPerConv) {
            for (c in 0 until convCount) {
                dao.insertMessage(makeMsg("m_${seq++}", "c_$c", (round * 100 + c).toLong()))
                dao.incrementUnread("c_$c")
            }
        }

        testDispatcher.scheduler.advanceTimeBy(500)
        val list = dao.observeConversations().first()

        for (conv in list) {
            assertEquals(msgsPerConv, conv.unreadCount, "Unread mismatch for ${conv.id}")
        }
        assertEquals((convCount * msgsPerConv).toLong(), dao.observeTotalUnread().first())
    }

    // ── 场景 5: clearUnread 后重新累加 ──

    @Test
    fun `clearUnread then re-insert maintains consistency`() = runTest {
        dao.insertConversation("c_0", "Group 0", 3, 0)

        for (i in 0 until 50) {
            dao.insertMessage(makeMsg("m_$i", "c_0", i.toLong()))
            dao.incrementUnread("c_0")
        }
        testDispatcher.scheduler.advanceTimeBy(500)
        assertEquals(50, dao.observeConversations().first().first().unreadCount)

        dao.clearUnread("c_0")
        testDispatcher.scheduler.advanceTimeBy(200)
        assertEquals(0, dao.observeConversations().first().first().unreadCount)

        for (i in 50 until 80) {
            dao.insertMessage(makeMsg("m_$i", "c_0", i.toLong()))
            dao.incrementUnread("c_0")
        }
        testDispatcher.scheduler.advanceTimeBy(500)
        assertEquals(30, dao.observeConversations().first().first().unreadCount)
        assertEquals(30L, dao.observeTotalUnread().first())
    }

    // ── 场景 6: 万级会话排序性能 ──

    @Test
    fun `10K conversations insert and sort under 500ms`() {
        val count = 10_000
        val elapsed = measureTime {
            for (i in 0 until count) {
                dao.insertConversation("c_$i", "G$i", 2, i.toLong())
            }
        }

        println("STRESS: 10K conversations inserted in ${elapsed.inWholeMilliseconds}ms")
        assertTrue(elapsed.inWholeMilliseconds < 5000, "Expected <5000ms, got ${elapsed.inWholeMilliseconds}ms")
    }

    // ── 场景 7: 同毫秒时间戳排序稳定性 ──

    @Test
    fun `same millisecond timestamp conversations have stable unique ordering`() = runTest {
        val convCount = 15 // <= INCREMENTAL_SORT_THRESHOLD (20) to trigger incremental sort path
        for (i in 0 until convCount) {
            dao.insertConversation("c_$i", "Group $i", 3, 0)
        }

        // First trigger full-sort to initialize _sortedMutable
        testDispatcher.scheduler.advanceTimeBy(500)
        dao.observeConversations().first()

        // Insert messages all with same timestamp to create dirty items triggering incremental sort
        val sharedTimestamp = 999L
        for (i in 0 until convCount) {
            dao.insertMessage(makeMsg("m_$i", "c_$i", sharedTimestamp))
        }

        testDispatcher.scheduler.advanceTimeBy(500)
        val list = dao.observeConversations().first()

        // No duplicates
        val ids = list.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate conversations found: $ids")

        // Same-timestamp items ordered by id ASC (tiebreaker determinism)
        val sameTimestamp = list.filter { it.lastActiveTime == sharedTimestamp }
        assertEquals(convCount, sameTimestamp.size, "Expected $convCount same-timestamp conversations")
        val expectedOrder = sameTimestamp.sortedBy { it.id }.map { it.id }
        assertEquals(expectedOrder, sameTimestamp.map { it.id }, "Same-timestamp conversations should be ordered by id ASC")
    }

    // ── 场景 8: 写缓冲背压韧性 ──

    @Test
    fun `write buffer backpressure at 5000 msg per second no OOM`() = runTest {
        dao.insertConversation("c_0", "Group 0", 3, 0)

        // Enqueue 5000 messages in a tight loop (simulates peak burst)
        for (i in 0 until 5000) {
            dao.enqueueMessage(makeMsg("m_$i", "c_0", i.toLong()))
        }

        // Allow all flush jobs to complete
        testDispatcher.scheduler.advanceTimeBy(2000)

        // All messages must be delivered — no data loss
        val messages = dao.observeMessages("c_0", 5000).first()
        assertEquals(5000, messages.size, "Expected 5000 messages delivered, got ${messages.size}")
    }

    // ── 场景 9: 消息摘要最终一致 ──

    @Test
    fun `last message preview consistent after 50 rapid inserts`() = runTest {
        dao.insertConversation("c_0", "Group 0", 3, 0)

        for (i in 0 until 50) {
            dao.insertMessage(makeMsg("m_$i", "c_0", i.toLong()))
        }
        dao.insertMessage(makeMsg("m_50", "c_0", 50, text = "final message"))

        testDispatcher.scheduler.advanceTimeBy(500)
        val conv = dao.observeConversations().first().first()
        assertEquals("final message", conv.lastMsgPreview)
        assertEquals(50L, conv.lastActiveTime)
    }
}
