@file:OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.archshowcase.chat.local

import com.example.archshowcase.chat.db.ChatDatabase
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.chat.mapper.deserializeBody
import com.example.archshowcase.chat.mapper.toBodyJson
import com.example.archshowcase.chat.mapper.toBodyType
import com.example.archshowcase.chat.mapper.toChatMessage
import com.example.archshowcase.chat.mapper.toPreviewText
import com.example.archshowcase.chat.mapper.toSummaryBodyType
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.model.SendStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.WindowAnchor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val MAX_DISPLAY_AVATARS = 9

data class MemberInsert(
    val memberId: String,
    val memberName: String,
    val memberAvatar: String,
    val sortOrder: Int
)

class ChatDao : KoinComponent {

    private val db: ChatDatabase by inject()
    private val msgQ get() = db.chatMessageQueries
    private val convQ get() = db.conversationQueries

    /** 消息变更通知：按 conversationId 精确触发，替代表级 asFlow() */
    private val messageChange = MutableSharedFlow<String>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** 会话列表变更通知：合并信号（只通知"有变化"），具体 id 在 dirtyIds 中 */
    private val conversationChange = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // ── Fix #1: MutableMap + synchronized 替代 copy-on-write，原地 put，零拷贝 ──

    private val cacheLock = CacheLock()
    private val conversationCache = mutableMapOf<String, Conversation>()
    private var sortedConversations: List<Conversation> = emptyList()
    private val _sortedMutable = mutableListOf<Conversation>() // 内部排序用，避免每次全量重建
    private var sortedDirty = true // true = 需要全量重建（初始加载 / invalidate）
    private val dirtyIds = mutableSetOf<String>() // 增量脏标记：写路径 O(1) 标脏，读路径增量排序
    private var totalUnreadCount: Long = 0L
    private var lastSnapshotDirtyCount = 0 // 上次快照处理的脏项数，用于自适应降频

    // ── Write Buffer: 分片攒批写入，减少 SQLite fsync + 降低锁竞争 ──

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private class WriteShard {
        val lock = CacheLock()
        val pendingMessages = mutableListOf<ChatMessage>()
        val pendingUnreadIncrements = mutableMapOf<String, Int>()
        var flushJob: Job? = null
    }

    private val writeShards = Array(WRITE_SHARD_COUNT) { WriteShard() }

    // ── Messages ──

    fun observeMessages(conversationId: String, limit: Long, offset: Long = 0): Flow<List<ChatMessage>> =
        messageChange
            .filter { it == conversationId }
            .onStart { emit(conversationId) }
            .sample(100)
            .mapLatest {
                msgQ.selectByConversation(conversationId, limit, offset)
                    .executeAsList()
                    .map { it.toChatMessage() }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    /**
     * reactive 窗口查询：anchor + messageChange → sample → queryWindow → distinctUntilChanged。
     * anchor 和 messageChange 合并采样 — 快速连续拖动时避免每帧查 DB，100ms 对用户无感知。
     */
    fun observeWindow(
        conversationId: String,
        anchor: StateFlow<WindowAnchor>,
        windowSize: Int = WINDOW_SIZE
    ): Flow<MessageWindow> =
        combine(
            messageChange.filter { it == conversationId }.onStart { emit(conversationId) },
            anchor
        ) { _, a -> a }
            .sample(100)
            .mapLatest { a -> queryWindow(conversationId, a, windowSize) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    /** 同步查询窗口数据（供 History applyToState 使用） */
    fun queryWindowSync(conversationId: String, anchor: WindowAnchor, windowSize: Int = WINDOW_SIZE): MessageWindow =
        queryWindow(conversationId, anchor, windowSize)

    private fun queryWindow(conversationId: String, anchor: WindowAnchor, windowSize: Int): MessageWindow {
        val halfWindow = windowSize / 2
        return when (anchor) {
            is WindowAnchor.Latest -> {
                val messages = msgQ.selectByConversation(conversationId, windowSize.toLong(), 0)
                    .executeAsList()
                    .map { it.toChatMessage() }
                val total = msgQ.countByConversation(conversationId).executeAsOne()
                MessageWindow(
                    messages = messages,
                    hasMoreBefore = messages.size.toLong() < total,
                    hasMoreAfter = false,
                    newMessageCount = 0
                )
            }
            is WindowAnchor.At -> {
                // afterCount +1 补偿 anchor 自身（SQL after 分支 id >= anchorId 含 anchor），
                // 总量 = halfWindow+1 + halfWindow = windowSize+1，居中布局常规做法
                val messages = msgQ.selectAround(
                    conversationId = conversationId,
                    anchorTs = anchor.timestamp,
                    anchorId = anchor.id,
                    afterCount = (halfWindow + 1).toLong(),
                    beforeCount = halfWindow.toLong()
                ).executeAsList().map { it.toChatMessage() }
                // hasMoreBefore: 查窗口最旧消息之前是否还有
                val oldest = messages.lastOrNull()
                val hasMoreBefore = if (oldest != null) {
                    msgQ.selectBeforeTimestamp(conversationId, oldest.timestamp, oldest.id, 1)
                        .executeAsList().isNotEmpty()
                } else false
                // hasMoreAfter: 基于 anchor 位置判断
                val afterAnchorTotal = msgQ.countAfterAnchor(
                    conversationId = conversationId,
                    anchorTs = anchor.timestamp,
                    anchorId = anchor.id
                ).executeAsOne()
                val afterAnchorInWindow = messages.count {
                    it.timestamp > anchor.timestamp || (it.timestamp == anchor.timestamp && it.id > anchor.id)
                }
                // newMessageCount: 基于 newestSeen（用户离开底部时的位置）计算真正的新消息
                // newestSeen=0 时退化为 anchor 位置（兼容测试和 TTE replay）
                val (countTs, countId) = if (anchor.newestSeenTs > 0L)
                    anchor.newestSeenTs to anchor.newestSeenId
                else anchor.timestamp to anchor.id
                val newCount = msgQ.countAfterAnchor(
                    conversationId = conversationId,
                    anchorTs = countTs,
                    anchorId = countId
                ).executeAsOne()
                val window = MessageWindow(
                    messages = messages,
                    hasMoreBefore = hasMoreBefore,
                    hasMoreAfter = afterAnchorTotal > afterAnchorInWindow,
                    newMessageCount = newCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                )
                window
            }
        }
    }

    /** keyset 分页：复合 cursor (timestamp, id) 避免同毫秒丢失，O(1) 索引查找 */
    fun loadMessagesBefore(conversationId: String, beforeTimestamp: Long, beforeId: String, limit: Long): List<ChatMessage> =
        msgQ.selectBeforeTimestamp(conversationId, beforeTimestamp, beforeId, limit)
            .executeAsList()
            .map { it.toChatMessage() }

    // ── Fix #2: db.transaction 包裹 INSERT + UPDATE，1 次 fsync ──

    fun insertMessage(msg: ChatMessage) {
        val preview = msg.body.toPreviewText()
        val bodyType = msg.body.toSummaryBodyType()
        db.transaction {
            msgQ.insert(
                id = msg.id,
                conversation_id = msg.conversationId,
                sender_id = msg.senderId,
                sender_name = msg.senderName,
                sender_avatar = msg.senderAvatar,
                body_type = msg.toBodyType(),
                body_json = msg.toBodyJson(),
                timestamp = msg.timestamp,
                is_mine = if (msg.isMine) 1L else 0L,
                status = msg.status.name,
                is_recalled = if (msg.isRecalled) 1L else 0L
            )
            convQ.updateConversationSummary(
                last_msg_preview = preview,
                last_msg_body_type = bodyType,
                last_msg_sender_name = msg.senderName,
                last_active_time = msg.timestamp,
                id = msg.conversationId
            )
        }
        updateCache(msg.conversationId, timestamp = msg.timestamp) { cached ->
            cached.copy(
                lastMsgPreview = preview,
                lastMsgBodyType = bodyType,
                lastMsgSenderName = msg.senderName,
                lastActiveTime = msg.timestamp
            )
        }
        messageChange.tryEmit(msg.conversationId)
        conversationChange.tryEmit(Unit)
    }

    /**
     * 写入队列：立即更新缓存（UI 秒响应），DB 写入按 conversationId 分片攒批 flush。
     * 每个分片独立 50ms 窗口，降低锁竞争 + 减少单次 flush 触发频率。
     */
    fun enqueueMessage(msg: ChatMessage, incrementUnread: Boolean = false) {
        // 1. 单次加锁：缓存更新 + totalUnreadCount 原子一致
        val preview = msg.body.toPreviewText()
        val bodyType = msg.body.toSummaryBodyType()
        cacheLock.withLock {
            conversationCache[msg.conversationId]?.let { cached ->
                conversationCache[msg.conversationId] = cached.copy(
                    lastMsgPreview = preview,
                    lastMsgBodyType = bodyType,
                    lastMsgSenderName = msg.senderName,
                    lastActiveTime = msg.timestamp,
                    unreadCount = if (incrementUnread) cached.unreadCount + 1 else cached.unreadCount
                )
                dirtyIds.add(msg.conversationId)
                if (incrementUnread) totalUnreadCount++
            }
        }
        conversationChange.tryEmit(Unit)

        // 2. 按 conversationId 路由到分片，有界缓冲（超限时同步 flush 施加背压）
        val shard = writeShards[msg.conversationId.hashCode().ushr(1) % WRITE_SHARD_COUNT]
        val needsSyncFlush = shard.lock.withLock {
            shard.pendingMessages.add(msg)
            if (incrementUnread) {
                shard.pendingUnreadIncrements[msg.conversationId] =
                    (shard.pendingUnreadIncrements[msg.conversationId] ?: 0) + 1
            }
            val overflow = shard.pendingMessages.size >= PENDING_MESSAGES_CAP
            if (!overflow && shard.flushJob?.isActive != true) {
                shard.flushJob = writeScope.launch {
                    delay(50)
                    flushShard(shard)
                }
            }
            overflow
        }
        if (needsSyncFlush) {
            Log.w(TAG) {
                "Write buffer overflow: shard flush forced synchronously " +
                "(pending=$PENDING_MESSAGES_CAP, convId=${msg.conversationId})"
            }
            flushShard(shard)
        }
    }

    private fun flushShard(shard: WriteShard) {
        val snapshot = shard.lock.withLock {
            if (shard.pendingMessages.isEmpty()) return@withLock null
            val m = shard.pendingMessages.toList()
            val u = shard.pendingUnreadIncrements.toMap()
            shard.pendingMessages.clear()
            shard.pendingUnreadIncrements.clear()
            m to u
        } ?: return
        val (msgs, unreads) = snapshot
        batchInsertToDb(msgs)
        if (unreads.isNotEmpty()) {
            db.transaction {
                for ((convId, count) in unreads) {
                    convQ.incrementUnreadBy(delta = count.toLong(), id = convId)
                }
            }
        }
        msgs.map { it.conversationId }.distinct().forEach { messageChange.tryEmit(it) }
    }

    private data class ConvSummarySnapshot(
        val preview: String, val bodyType: String,
        val senderName: String, val timestamp: Long
    )

    /** DB-only 批量插入：单事务，返回每个会话的最新摘要 */
    private fun batchInsertToDb(messages: List<ChatMessage>): Map<String, ConvSummarySnapshot> {
        val latestByConv = mutableMapOf<String, ConvSummarySnapshot>()
        db.transaction {
            for (msg in messages) {
                msgQ.insert(
                    id = msg.id,
                    conversation_id = msg.conversationId,
                    sender_id = msg.senderId,
                    sender_name = msg.senderName,
                    sender_avatar = msg.senderAvatar,
                    body_type = msg.toBodyType(),
                    body_json = msg.toBodyJson(),
                    timestamp = msg.timestamp,
                    is_mine = if (msg.isMine) 1L else 0L,
                    status = msg.status.name,
                    is_recalled = if (msg.isRecalled) 1L else 0L
                )
                val preview = msg.body.toPreviewText()
                val bodyType = msg.body.toSummaryBodyType()
                val existing = latestByConv[msg.conversationId]
                if (existing == null || msg.timestamp > existing.timestamp) {
                    latestByConv[msg.conversationId] = ConvSummarySnapshot(preview, bodyType, msg.senderName, msg.timestamp)
                }
            }
            for ((convId, snap) in latestByConv) {
                convQ.updateConversationSummary(
                    last_msg_preview = snap.preview,
                    last_msg_body_type = snap.bodyType,
                    last_msg_sender_name = snap.senderName,
                    last_active_time = snap.timestamp,
                    id = convId
                )
            }
        }
        return latestByConv
    }

    /** 批量插入消息：单事务 + 单次通知，避免万条消息万次 fsync */
    fun insertMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val affectedConversations = messages.map { it.conversationId }.toMutableSet()
        val latestByConv = batchInsertToDb(messages)
        // 复用已计算的 summary 更新缓存
        cacheLock.withLock {
            for ((convId, snap) in latestByConv) {
                conversationCache[convId]?.let { cached ->
                    conversationCache[convId] = cached.copy(
                        lastMsgPreview = snap.preview,
                        lastMsgBodyType = snap.bodyType,
                        lastMsgSenderName = snap.senderName,
                        lastActiveTime = snap.timestamp
                    )
                }
            }
            dirtyIds.addAll(latestByConv.keys)
        }
        for (convId in affectedConversations) {
            messageChange.tryEmit(convId)
            conversationChange.tryEmit(Unit)
        }
    }

    fun updateStatus(messageId: String, status: SendStatus, conversationId: String? = null) {
        val convId = conversationId ?: msgQ.selectById(messageId).executeAsOneOrNull()?.conversation_id
        msgQ.updateStatus(status = status.name, id = messageId)
        convId?.let {
            messageChange.tryEmit(it)
            conversationChange.tryEmit(Unit)
        }
    }

    fun markRecalled(messageId: String, conversationId: String? = null) {
        val convId = conversationId ?: msgQ.selectById(messageId).executeAsOneOrNull()?.conversation_id
        msgQ.markRecalled(id = messageId)
        convId?.let {
            syncConversationSummary(it)
            messageChange.tryEmit(it)
            conversationChange.tryEmit(Unit)
        }
    }

    fun deleteMessage(messageId: String, conversationId: String? = null) {
        val convId = conversationId ?: msgQ.selectById(messageId).executeAsOneOrNull()?.conversation_id
        msgQ.deleteById(id = messageId)
        convId?.let {
            syncConversationSummary(it)
            messageChange.tryEmit(it)
            conversationChange.tryEmit(Unit)
        }
    }

    fun deleteMessages(conversationId: String) {
        msgQ.deleteByConversation(conversationId = conversationId)
        syncConversationSummary(conversationId)
        messageChange.tryEmit(conversationId)
        conversationChange.tryEmit(Unit)
    }

    fun getLatestMessage(conversationId: String): ChatMessage? =
        msgQ.latestByConversation(conversationId).executeAsOneOrNull()?.toChatMessage()

    /** 查最新未撤回消息并更新 conversation 反规范化字段 + 缓存 */
    fun syncConversationSummary(conversationId: String) {
        val latestRow = msgQ.latestNonRecalledByConversation(conversationId).executeAsOneOrNull()
        if (latestRow != null) {
            val body = deserializeBody(latestRow.body_type, latestRow.body_json)
            val preview = body.toPreviewText()
            val bodyType = body.toSummaryBodyType()
            convQ.updateConversationSummary(
                last_msg_preview = preview,
                last_msg_body_type = bodyType,
                last_msg_sender_name = latestRow.sender_name,
                last_active_time = latestRow.timestamp,
                id = conversationId
            )
            updateCache(conversationId) { cached ->
                cached.copy(
                    lastMsgPreview = preview,
                    lastMsgBodyType = bodyType,
                    lastMsgSenderName = latestRow.sender_name,
                    lastActiveTime = latestRow.timestamp
                )
            }
        } else {
            convQ.updateConversationSummary(
                last_msg_preview = null,
                last_msg_body_type = null,
                last_msg_sender_name = null,
                last_active_time = 0,
                id = conversationId
            )
            updateCache(conversationId) { cached ->
                cached.copy(
                    lastMsgPreview = null,
                    lastMsgBodyType = null,
                    lastMsgSenderName = null,
                    lastActiveTime = 0
                )
            }
        }
    }

    // ── Conversations ──

    // Fix #3: dirty flag — 写 O(1) 标脏，自适应降频排序
    fun observeConversations(): Flow<List<Conversation>> =
        conversationChange
            .onStart { emit(Unit) }
            .sample(100)
            .transformLatest {
                ensureCacheLoaded()
                val result = buildSortedSnapshot()
                emit(result)
                // 高负载时额外冷却：延长采样间隔，降低 recompose 频率
                if (lastSnapshotDirtyCount > ADAPTIVE_SAMPLE_THRESHOLD) {
                    delay(200) // 总间隔 ≈ 300ms
                }
            }
            .distinctUntilChanged { old, new -> old === new }
            .flowOn(Dispatchers.Default)

    fun insertConversation(
        id: String,
        name: String,
        memberCount: Int,
        lastActiveTime: Long,
        lastMsgPreview: String? = null,
        lastMsgBodyType: String? = null,
        lastMsgSenderName: String? = null
    ) {
        convQ.insertConversation(
            id = id,
            name = name,
            member_count = memberCount.toLong(),
            last_active_time = lastActiveTime,
            unread_count = 0,
            last_msg_preview = lastMsgPreview,
            last_msg_body_type = lastMsgBodyType,
            last_msg_sender_name = lastMsgSenderName
        )
        cacheLock.withLock {
            if (conversationCache.isNotEmpty()) {
                val existing = conversationCache[id]
                conversationCache[id] = Conversation(
                    id = id,
                    name = name,
                    memberAvatars = existing?.memberAvatars ?: emptyList(),
                    memberCount = memberCount,
                    lastActiveTime = lastActiveTime,
                    unreadCount = 0,
                    lastMsgPreview = lastMsgPreview,
                    lastMsgBodyType = lastMsgBodyType,
                    lastMsgSenderName = lastMsgSenderName
                )
                dirtyIds.add(id)
            }
        }
        conversationChange.tryEmit(Unit)
    }

    fun insertMember(
        conversationId: String,
        memberId: String,
        memberName: String,
        memberAvatar: String,
        sortOrder: Int
    ) {
        convQ.insertMember(
            conversation_id = conversationId,
            member_id = memberId,
            member_name = memberName,
            member_avatar = memberAvatar,
            sort_order = sortOrder.toLong()
        )
        if (sortOrder < MAX_DISPLAY_AVATARS) {
            updateCache(conversationId) { cached ->
                cached.copy(
                    memberAvatars = (cached.memberAvatars + memberAvatar).take(MAX_DISPLAY_AVATARS)
                )
            }
        }
        conversationChange.tryEmit(Unit)
    }

    // Fix #3: incrementUnread/clearUnread 同步维护 totalUnreadCount 内存计数器

    fun incrementUnread(conversationId: String) {
        convQ.incrementUnread(id = conversationId)
        cacheLock.withLock {
            conversationCache[conversationId]?.let { cached ->
                conversationCache[conversationId] = cached.copy(unreadCount = cached.unreadCount + 1)
                totalUnreadCount++
                dirtyIds.add(conversationId)
            }
        }
        conversationChange.tryEmit(Unit)
    }

    fun clearUnread(conversationId: String) {
        convQ.clearUnread(id = conversationId)
        cacheLock.withLock {
            conversationCache[conversationId]?.let { cached ->
                totalUnreadCount -= cached.unreadCount
                conversationCache[conversationId] = cached.copy(unreadCount = 0)
                dirtyIds.add(conversationId)
            }
        }
        conversationChange.tryEmit(Unit)
    }

    fun updateLastActive(conversationId: String, time: Long) {
        convQ.updateLastActive(time = time, id = conversationId)
        updateCache(conversationId) { cached ->
            cached.copy(lastActiveTime = time)
        }
        conversationChange.tryEmit(Unit)
    }

    /** 强制从 DB 重新加载缓存（App 从后台恢复时调用） */
    fun invalidateCache() {
        cacheLock.withLock {
            conversationCache.clear()
            sortedConversations = emptyList()
            _sortedMutable.clear()
            dirtyIds.clear()
            sortedDirty = true
            totalUnreadCount = 0
        }
        conversationChange.tryEmit(Unit)
    }

    // Fix #3: observeTotalUnread 从内存计数器取值；缓存未加载时 fallback 到锁外 DB 查询
    fun observeTotalUnread(): Flow<Long> =
        conversationChange
            .onStart { emit(Unit) }
            .sample(100)
            .mapLatest {
                val (loaded, count) = cacheLock.withLock {
                    (conversationCache.isNotEmpty()) to totalUnreadCount
                }
                if (loaded) count else convQ.totalUnread().executeAsOne()
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    /** 批量插入成员：单事务 + 单次通知，只缓存前 N 个头像 */
    fun insertMembersForConversation(conversationId: String, members: List<MemberInsert>) {
        if (members.isEmpty()) return
        db.transaction {
            for (m in members) {
                convQ.insertMember(conversationId, m.memberId, m.memberName, m.memberAvatar, m.sortOrder.toLong())
            }
        }
        cacheLock.withLock {
            conversationCache[conversationId]?.let { cached ->
                val avatars = members
                    .filter { it.sortOrder < MAX_DISPLAY_AVATARS }
                    .sortedBy { it.sortOrder }
                    .map { it.memberAvatar }
                    .take(MAX_DISPLAY_AVATARS)
                conversationCache[conversationId] = cached.copy(memberAvatars = avatars)
                dirtyIds.add(conversationId)
            }
        }
        conversationChange.tryEmit(Unit)
    }

    // ── Private ──

    /** Fix #1: withLock + 原地 put，O(1) 零拷贝；标脏不排序 */
    private fun updateCache(
        conversationId: String,
        timestamp: Long = 0L,
        transform: (Conversation) -> Conversation
    ) {
        val needsDbLoad = cacheLock.withLock {
            val existing = conversationCache[conversationId]
            when {
                existing != null -> {
                    conversationCache[conversationId] = transform(existing)
                    dirtyIds.add(conversationId)
                    false
                }
                conversationCache.isNotEmpty() -> true  // cache loaded but entry missing
                else -> false  // cache not yet loaded
            }
        }
        if (!needsDbLoad) return
        // Race: cache loaded but this conversation missed — load from DB
        val row = convQ.selectById(conversationId).executeAsOneOrNull() ?: return
        val avatars = convQ.selectMembers(conversationId).executeAsList()
            .filter { it.sort_order < MAX_DISPLAY_AVATARS }
            .map { it.member_avatar }
        val loaded = Conversation(
            id = row.id, name = row.name, memberAvatars = avatars,
            memberCount = row.member_count.toInt(), lastActiveTime = row.last_active_time,
            unreadCount = row.unread_count.toInt(), lastMsgPreview = row.last_msg_preview,
            lastMsgBodyType = row.last_msg_body_type, lastMsgSenderName = row.last_msg_sender_name
        )
        cacheLock.withLock {
            val current = conversationCache[conversationId]
            // ABA 防护：如果已有值比本次更新更新，跳过
            if (current != null && timestamp > 0L && current.lastActiveTime > timestamp) return@withLock
            conversationCache[conversationId] = transform(current ?: loaded)
            dirtyIds.add(conversationId)
        }
    }

    /**
     * 全量排序在锁外执行：写路径只被短暂快照阻塞（~0.1ms），不被排序阻塞（~2ms）。
     * 增量排序（D ≤ THRESHOLD）仍在锁内，因为总操作量 < 全量快照成本。
     */
    private fun buildSortedSnapshot(): List<Conversation> {
        // Phase 1: 锁内快速判断 + 必要时快照
        val fullSnapshot = cacheLock.withLock {
            when {
                sortedDirty -> {
                    lastSnapshotDirtyCount = conversationCache.size
                    val snap = conversationCache.values.toList()
                    sortedDirty = false
                    dirtyIds.clear()
                    snap
                }
                dirtyIds.isEmpty() -> {
                    lastSnapshotDirtyCount = 0
                    null
                }
                dirtyIds.size > INCREMENTAL_SORT_THRESHOLD -> {
                    lastSnapshotDirtyCount = dirtyIds.size
                    val snap = conversationCache.values.toList()
                    dirtyIds.clear()
                    snap
                }
                else -> {
                    lastSnapshotDirtyCount = dirtyIds.size
                    // 增量：D ≤ 20，锁内完成
                    incrementalSort()
                    null
                }
            }
        }
        if (fullSnapshot == null) return sortedConversations

        // Phase 2: 全量排序在锁外（不阻塞 enqueueMessage 写入）
        val sorted = fullSnapshot.sortedWith(CONVERSATION_COMPARATOR).toList()

        // Phase 3: 短暂加锁写回（若排序期间 cache 被 invalidate 则跳过）
        cacheLock.withLock {
            if (!sortedDirty) {
                _sortedMutable.clear()
                _sortedMutable.addAll(sorted)
                sortedConversations = sorted
            }
        }
        return sorted
    }

    /** 增量排序：在 cacheLock 内调用，D ≤ THRESHOLD */
    private fun incrementalSort() {
        _sortedMutable.removeAll { it.id in dirtyIds }
        for (id in dirtyIds) {
            val conv = conversationCache[id] ?: continue
            val searchResult = _sortedMutable.binarySearch(conv, CONVERSATION_COMPARATOR)
            val insertIdx = if (searchResult >= 0) searchResult else -(searchResult + 1)
            _sortedMutable.add(insertIdx, conv)
        }
        sortedConversations = _sortedMutable.toList()
        dirtyIds.clear()
    }

    /** DB I/O 在锁外分批执行，峰值 = O(batchSize) 而非 O(全量)；double-check 防并发重复加载。 */
    private fun ensureCacheLoaded() {
        val needsLoad = cacheLock.withLock { conversationCache.isEmpty() }
        if (!needsLoad) return

        val batchSize = 500L
        var offset = 0L
        val tempCache = mutableMapOf<String, Conversation>()
        var unread = 0L

        while (true) {
            val rows = convQ.selectPaged(batchSize, offset).executeAsList()
            if (rows.isEmpty()) break
            val ids = rows.map { it.id }
            val members = convQ.selectMembersForConversations(ids).executeAsList()
                .groupBy { it.conversation_id }
            for (row in rows) {
                val avatars = members[row.id]?.map { it.member_avatar }
                    ?.take(MAX_DISPLAY_AVATARS) ?: emptyList()
                tempCache[row.id] = Conversation(
                    id = row.id,
                    name = row.name,
                    memberAvatars = avatars,
                    memberCount = row.member_count.toInt(),
                    lastActiveTime = row.last_active_time,
                    unreadCount = row.unread_count.toInt(),
                    lastMsgPreview = row.last_msg_preview,
                    lastMsgBodyType = row.last_msg_body_type,
                    lastMsgSenderName = row.last_msg_sender_name
                )
                unread += row.unread_count
            }
            offset += batchSize
            // rows / members 超出作用域后可被 GC，峰值 = 单批大小
        }

        cacheLock.withLock {
            if (conversationCache.isNotEmpty()) return@withLock // double-check
            conversationCache.putAll(tempCache)
            totalUnreadCount = unread
            sortedDirty = true
        }
    }

    companion object {
        private const val TAG = "ChatDao"
        /** 共享比较器：时间降序 + id 升序 tiebreak，全量/增量排序结果一致 */
        private val CONVERSATION_COMPARATOR: Comparator<Conversation> =
            compareByDescending<Conversation> { it.lastActiveTime }
                .thenBy { it.id }

        /** 增量排序阈值：D > log₂(N) 时增量 O(D×N) 超过全量 O(N·logN) */
        private const val INCREMENTAL_SORT_THRESHOLD = 20
        /** 写入分片数：不同 conversationId 路由到不同分片，降低锁竞争 */
        private const val WRITE_SHARD_COUNT = 4
        /** 每个分片的写入队列上限：超限时同步 flush，施加背压防 OOM */
        private const val PENDING_MESSAGES_CAP = 1000
        /** 自适应降频阈值：dirtyIds 超过此值时延长采样间隔到 ~300ms */
        private const val ADAPTIVE_SAMPLE_THRESHOLD = 50
        /** 消息窗口默认大小 */
        const val WINDOW_SIZE = MessageWindow.DEFAULT_WINDOW_SIZE
    }
}
