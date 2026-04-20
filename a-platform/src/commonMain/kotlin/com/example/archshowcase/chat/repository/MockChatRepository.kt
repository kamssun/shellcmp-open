@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.chat.repository

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.chat.local.ChatDao
import com.example.archshowcase.chat.local.MemberInsert
import com.example.archshowcase.chat.mapper.toPreviewText
import com.example.archshowcase.chat.mapper.toSummaryBodyType
import com.example.archshowcase.chat.mock.MockDataGenerator
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.MockMember
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.chat.model.WindowAnchor
import com.example.archshowcase.chat.stress.StressTestConfig
import com.example.archshowcase.chat.stress.StressTestLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.concurrent.Volatile
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.TimeSource

class MockChatRepository : ChatRepository, KoinComponent, AutoCloseable {

    private val dao: ChatDao by inject()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private val groupMembers = mutableMapOf<String, List<MockMember>>()
    private val groupNames = mutableMapOf<String, String>()
    private val typingStates = mutableMapOf<String, MutableStateFlow<Pair<Boolean, String?>>>()

    // Currently active conversation (for deciding unread vs not)
    @Volatile
    private var activeConversationId: String? = null

    override val conversations: Flow<List<Conversation>> = dao.observeConversations()
    override val totalUnread: Flow<Long> = dao.observeTotalUnread()

    init {
        scope.launch {
            seedData()
            // 压测自动启动：STRESS_CONFIG 非 null 时执行
            STRESS_CONFIG?.let { config ->
                delay(3000)
                startStressTest(config)
            }
        }
    }

    private fun seedData() {
        val groups = MockDataGenerator.generateGroups()
        for (group in groups) {
            groupNames[group.id] = group.name
            groupMembers[group.id] = group.members

            // 清除上次运行遗留的 simulation 消息，保证每次启动数据一致
            dao.deleteMessages(group.id)

            val lastMsg = group.messages.lastOrNull()
            dao.insertConversation(
                id = group.id,
                name = group.name,
                memberCount = group.members.size + 1, // +1 for local user
                lastActiveTime = lastMsg?.timestamp ?: 0,
                lastMsgPreview = lastMsg?.body?.toPreviewText(),
                lastMsgBodyType = lastMsg?.body?.toSummaryBodyType(),
                lastMsgSenderName = lastMsg?.senderName
            )
            dao.insertMembersForConversation(
                group.id,
                group.members.mapIndexed { idx, member ->
                    MemberInsert(member.id, member.name, member.avatar, idx)
                }
            )
            dao.insertMessages(group.messages) // 批量写入：单事务 + 单次通知
        }
        Log.d(TAG) { "Seeded ${groups.size} groups with mock data" }
    }

    override fun observeMessages(
        conversationId: String,
        anchor: StateFlow<WindowAnchor>,
        windowSize: Int
    ): Flow<MessageWindow> = dao.observeWindow(conversationId, anchor, windowSize)

    override suspend fun sendMessage(conversationId: String, body: MessageBody): Result<ChatMessage> {
        val now = Clock.System.now().toEpochMilliseconds()
        val msg = ChatMessage(
            id = "sent_${conversationId}_$now",
            conversationId = conversationId,
            senderId = MockDataGenerator.LOCAL_USER_ID,
            senderName = MockDataGenerator.LOCAL_USER_NAME,
            senderAvatar = null,
            body = body,
            timestamp = now,
            isMine = true,
            status = SendStatus.SENDING
        )
        dao.insertMessage(msg)
        dao.updateLastActive(conversationId, now)

        // Simulate network delay then mark sent
        scope.launch {
            delay(300)
            dao.updateStatus(msg.id, SendStatus.SENT, conversationId)
            // Trigger auto-reply
            scheduleAutoReply(conversationId)
        }
        return Result.success(msg)
    }

    override suspend fun resendMessage(messageId: String, conversationId: String): Result<ChatMessage> {
        dao.updateStatus(messageId, SendStatus.SENDING, conversationId)
        scope.launch {
            delay(500)
            dao.updateStatus(messageId, SendStatus.SENT, conversationId)
        }
        // Return minimal ack; callers observe real state via Flow, not this return value.
        return Result.success(
            ChatMessage(
                id = messageId, conversationId = conversationId, senderId = MockDataGenerator.LOCAL_USER_ID,
                senderName = MockDataGenerator.LOCAL_USER_NAME, senderAvatar = null, body = MessageBody.Text(""),
                timestamp = Clock.System.now().toEpochMilliseconds(), isMine = true,
                status = SendStatus.SENDING
            )
        )
    }

    override suspend fun loadMoreHistory(
        conversationId: String,
        beforeTimestamp: Long,
        beforeId: String
    ): Result<List<ChatMessage>> {
        delay(200)
        val messages = dao.loadMessagesBefore(conversationId, beforeTimestamp, beforeId, limit = 30)
        return Result.success(messages)
    }

    override suspend fun recallMessage(messageId: String, conversationId: String): Result<Unit> {
        dao.markRecalled(messageId, conversationId)
        return Result.success(Unit)
    }

    override suspend fun deleteMessage(messageId: String, conversationId: String): Result<Unit> {
        dao.deleteMessage(messageId, conversationId)
        return Result.success(Unit)
    }

    override fun markRead(conversationId: String) {
        activeConversationId = conversationId
        dao.clearUnread(conversationId)
    }

    override fun getConversationName(conversationId: String): String? {
        ensureConversationExists(conversationId)
        return groupNames[conversationId]
    }

    override fun getMemberCount(conversationId: String): Int {
        ensureConversationExists(conversationId)
        return (groupMembers[conversationId]?.size ?: 0) + 1
    }

    /** 未知 conversationId 时自动 seed 一组预览数据（Preview / 直接跳转场景） */
    private fun ensureConversationExists(conversationId: String) {
        if (conversationId in groupNames) return
        val group = MockDataGenerator.generateSingleGroup(conversationId)
        groupNames[group.id] = group.name
        groupMembers[group.id] = group.members
        val lastMsg = group.messages.lastOrNull()
        dao.insertConversation(
            id = group.id,
            name = group.name,
            memberCount = group.members.size + 1,
            lastActiveTime = lastMsg?.timestamp ?: 0,
            lastMsgPreview = lastMsg?.body?.toPreviewText(),
            lastMsgBodyType = lastMsg?.body?.toSummaryBodyType(),
            lastMsgSenderName = lastMsg?.senderName
        )
        dao.insertMembersForConversation(
            group.id,
            group.members.mapIndexed { idx, member ->
                MemberInsert(member.id, member.name, member.avatar, idx)
            }
        )
        dao.insertMessages(group.messages)
    }

    override fun observeTypingState(conversationId: String): StateFlow<Pair<Boolean, String?>> =
        typingStates.getOrPut(conversationId) {
            MutableStateFlow(Pair(false, null))
        }.asStateFlow()

    private fun scheduleAutoReply(conversationId: String) {
        val members = groupMembers[conversationId] ?: return
        scope.launch {
            // Show typing indicator
            val typingFlow = typingStates.getOrPut(conversationId) {
                MutableStateFlow(Pair(false, null))
            }
            val reply = MockDataGenerator.generateAutoReply(conversationId, members)
            delay(800 + (Random.nextDouble() * 2000).toLong())
            typingFlow.value = Pair(true, reply.senderName)
            delay(500 + (Random.nextDouble() * 1500).toLong())
            typingFlow.value = Pair(false, null)

            dao.enqueueMessage(reply, incrementUnread = activeConversationId != conversationId)
        }
    }

    override fun onLeaveConversation() {
        activeConversationId = null
    }

    // ── 压力测试 ──────────────────────────────────────────────────

    fun startStressTest(config: StressTestConfig) {
        Log.d(TAG) { "┌── StressTest START ──────────────────────────" }
        Log.d(TAG) { "│ groups=${config.totalGroups} active=${config.activeGroups}" }
        Log.d(TAG) { "│ rate=${config.msgPerSecPerGroup}msg/s/group  total≈${config.theoreticalThroughput.toInt()}msg/s" }
        Log.d(TAG) { "│ duration=${config.durationMs / 1000}s  chatRoom=${config.openChatRoomId ?: "none"}" }
        Log.d(TAG) { "└──────────────────────────────────────────────" }

        StressTestLogger.enabled = true
        StressTestLogger.reset()

        // Phase 1: Seed 压测群
        seedStressGroups(config)

        // Phase 2: 消息注入
        startMessageInjection(config)

        // Phase 3: ChatRoom 高频注入
        config.openChatRoomId?.let { roomId ->
            startChatRoomInjection(roomId, config)
        }

        // Phase 4: 定期日志 + 最终 summary
        scope.launch {
            val deadline = TimeSource.Monotonic.markNow()
            var elapsed = 0L
            while (elapsed < config.durationMs) {
                delay(config.logIntervalMs)
                elapsed = deadline.elapsedNow().inWholeMilliseconds
                StressTestLogger.printSnapshot()
            }
            StressTestLogger.printFinalSummary()
            StressTestLogger.enabled = false
            Log.d(TAG) { "StressTest COMPLETE" }
        }
    }

    private fun seedStressGroups(config: StressTestConfig) {
        val existingCount = groupNames.size
        if (config.totalGroups <= existingCount) {
            Log.d(TAG) { "SEED skip: already have $existingCount groups (need ${config.totalGroups})" }
            return
        }

        val mark = TimeSource.Monotonic.markNow()
        val groups = MockDataGenerator.generateStressGroups(config.totalGroups, config.messagesPerGroup)
        var seeded = 0

        for (group in groups) {
            if (group.id in groupNames) continue // 已有，跳过

            groupNames[group.id] = group.name
            groupMembers[group.id] = group.members

            dao.insertConversation(
                id = group.id,
                name = group.name,
                memberCount = group.members.size + 1,
                lastActiveTime = 0,
                lastMsgPreview = null,
                lastMsgBodyType = null,
                lastMsgSenderName = null
            )
            dao.insertMembersForConversation(
                group.id,
                group.members.mapIndexed { idx, member ->
                    MemberInsert(member.id, member.name, member.avatar, idx)
                }
            )
            if (group.messages.isNotEmpty()) {
                dao.insertMessages(group.messages)
            }

            seeded++
            if (seeded % 100 == 0) {
                val elapsed = mark.elapsedNow().inWholeMilliseconds
                Log.d(TAG) { "SEED [$seeded/${config.totalGroups - existingCount}] ${elapsed}ms" }
            }
        }

        val totalMs = mark.elapsedNow().inWholeMilliseconds
        Log.d(TAG) { "SEED complete: +$seeded groups in ${totalMs}ms (total ${groupNames.size})" }
    }

    private fun startMessageInjection(config: StressTestConfig) {
        val activeIds = groupNames.keys.take(config.activeGroups)
        val batchSize = 20 // 每个协程负责 20 个群
        val intervalMs = (1000.0 / config.msgPerSecPerGroup).toLong().coerceAtLeast(1)

        for ((batchIdx, batch) in activeIds.chunked(batchSize).withIndex()) {
            scope.launch {
                val deadline = TimeSource.Monotonic.markNow()
                var seq = batchIdx * 1_000_000L // 每个协程独立序号空间

                while (deadline.elapsedNow().inWholeMilliseconds < config.durationMs) {
                    for (convId in batch) {
                        val members = groupMembers[convId] ?: continue
                        val msg = MockDataGenerator.generateStressMessage(convId, members, seq++)
                        val mark = StressTestLogger.markStart()
                        dao.enqueueMessage(msg, incrementUnread = convId != activeConversationId)
                        StressTestLogger.recordEnqueue(mark)
                    }
                    delay(intervalMs)
                }
            }
        }
        Log.d(TAG) { "Injection started: ${activeIds.size} groups in ${activeIds.chunked(batchSize).size} coroutines, interval=${intervalMs}ms" }
    }

    private fun startChatRoomInjection(roomId: String, config: StressTestConfig) {
        val members = groupMembers[roomId]
        if (members == null) {
            Log.d(TAG) { "ChatRoom injection skipped: $roomId not found" }
            return
        }

        val intervalMs = (1000.0 / config.chatRoomMsgPerSec).toLong().coerceAtLeast(1)
        scope.launch {
            val deadline = TimeSource.Monotonic.markNow()
            var seq = 0L
            while (deadline.elapsedNow().inWholeMilliseconds < config.durationMs) {
                val msg = MockDataGenerator.generateStressMessage(roomId, members, seq++)
                val mark = StressTestLogger.markStart()
                dao.enqueueMessage(msg, incrementUnread = false)
                StressTestLogger.recordEnqueue(mark)
                delay(intervalMs)
            }
        }
        Log.d(TAG) { "ChatRoom injection started: $roomId at ${config.chatRoomMsgPerSec}msg/s" }
    }

    override fun close() {
        job.cancel()
    }

    companion object {
        private const val TAG = "MockChatRepo"

        /**
         * 压测配置。需要压测时改为具体场景，不压测时设为 null。
         *
         * 可选场景：
         * - StressTestConfig.messageStorm() → 500 群 200 活跃 ~400msg/s
         * - StressTestConfig.chatRoomFlood() → 单群洪峰 50msg/s + 背景 50msg/s
         * - StressTestConfig.backpressure() → 背压极限 ~5000msg/s
         * - StressTestConfig.fullBlast() → 1000 群 300 活跃 + ChatRoom ~610msg/s
         */
        private val STRESS_CONFIG: StressTestConfig? = null
    }
}
