@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.chat.mock

import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MockMember
import com.example.archshowcase.chat.model.SendStatus
import kotlin.time.Clock

object MockDataGenerator {

    const val LOCAL_USER_ID = "local_user"
    internal const val LOCAL_USER_NAME = "我"

    // Fixed base time (2025-01-15 12:00 UTC) — intentionally in the past so
    // ConversationListContent.formatTime() always renders "M/d" format,
    // keeping Roborazzi screenshots deterministic regardless of run date.
    private const val SEED_BASE_TIME = 1_736_942_400_000L

    private val groupDefs = listOf(
        GroupDef("g1", "产品讨论组(5000条)", 3),
        GroupDef("g2", "周末聚餐", 5),
        GroupDef("g3", "项目 Alpha", 8),
        GroupDef("g4", "设计评审", 9),
        GroupDef("g5", "技术分享", 12),
        GroupDef("g6", "运营日报", 4),
        GroupDef("g7", "客户反馈", 6),
        GroupDef("g8", "市场推广", 7),
        GroupDef("g9", "新人培训", 3),
        GroupDef("g10", "All Hands", 12),
        GroupDef("g11", "读书会", 5),
        GroupDef("g12", "下午茶群", 8),
        GroupDef("g13", "周报提交", 9),
        GroupDef("g14", "旅游计划", 4),
    )

    private val memberNames = listOf(
        "张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十",
        "冯一", "陈二", "楚三", "魏四", "蒋五", "沈六", "韩七", "杨八",
        "朱九", "秦十", "许一", "何二", "吕三", "施四", "余五", "尤六"
    )

    private val textMessages = listOf(
        "大家好，今天的会议几点开始？",
        "我看了一下需求文档，有几个问题想讨论",
        "好的，收到 👍",
        "这个方案我觉得可以，先推进吧",
        "明天下午有空吗？想约个 code review",
        "已经提了 PR，麻烦帮忙看一下",
        "设计稿更新了，大家看看新版本",
        "接口联调完了，目前没有问题",
        "这个 bug 我来修，预计今天搞定",
        "周五团建大家有什么想法？",
        "辛苦了，先下班吧",
        "刚测了一下，性能提升明显 🎉",
        "报告写好了，发到群里了",
        "明天早上 10 点 standup",
        "新版本已经部署到测试环境",
        "等一下，我确认一下这个逻辑",
        "OK，那就这样定了",
        "图片我放在共享文件夹了",
        "这个需求优先级调高了",
        "有人用过这个库吗？感觉挺不错的",
    )

    fun generateGroups(): List<GroupData> = groupDefs.mapIndexed { gIdx, def ->
        val members = generateMembers(def.memberCount, gIdx)
        // g1 生成 5000 条用于窗口化压测，其余保持 100
        val count = if (def.id == "g1") 5000 else 100
        val messages = generateMessages(def.id, members, count = count, groupIndex = gIdx)
        GroupData(
            id = def.id,
            name = def.name,
            members = members,
            messages = messages
        )
    }

    /** 为未知 conversationId 生成一组预览数据 */
    fun generateSingleGroup(conversationId: String): GroupData {
        val seed = conversationId.hashCode().toLong().and(0x7FFFFFFF)
        val members = generateMembers(5, seed.toInt())
        val messages = generateMessages(conversationId, members, count = 20, groupIndex = seed.toInt())
        return GroupData(id = conversationId, name = "群聊 $conversationId", members = members, messages = messages)
    }

    private fun generateMembers(count: Int, groupSeed: Int): List<MockMember> {
        val result = mutableListOf<MockMember>()
        val namePool = memberNames.toMutableList()
        // Shuffle deterministically based on groupSeed
        val rng = SeededRandom(groupSeed.toLong() * 31 + 7)
        rng.shuffle(namePool)
        for (i in 0 until (count - 1).coerceAtMost(namePool.size)) {
            val name = namePool[i]
            result.add(
                MockMember(
                    id = "member_${groupSeed}_$i",
                    name = name,
                    avatar = "https://picsum.photos/seed/avatar_${groupSeed}_$i/100/100"
                )
            )
        }
        return result
    }

    private fun generateMessages(
        conversationId: String,
        members: List<MockMember>,
        count: Int,
        groupIndex: Int
    ): List<ChatMessage> {
        // Fixed base time for deterministic Roborazzi screenshots
        val now = SEED_BASE_TIME
        val rng = SeededRandom(groupIndex.toLong() * 1000 + 42)
        val messages = mutableListOf<ChatMessage>()

        for (i in 0 until count) {
            val timeOffset = (count - i) * 60_000L + rng.nextInt(30_000).toLong()
            val timestamp = now - timeOffset
            val isMine = rng.nextInt(5) == 0 // 20% 是自己发的
            val sender = if (isMine) null else members[rng.nextInt(members.size)]
            val body = generateMessageBody(i, groupIndex, rng)

            messages.add(
                ChatMessage(
                    id = "msg_${conversationId}_$i",
                    conversationId = conversationId,
                    senderId = if (isMine) LOCAL_USER_ID else sender!!.id,
                    senderName = if (isMine) LOCAL_USER_NAME else sender!!.name,
                    senderAvatar = sender?.avatar,
                    body = body,
                    timestamp = timestamp,
                    isMine = isMine,
                    status = SendStatus.SENT
                )
            )
        }
        return messages.sortedBy { it.timestamp }
    }

    private fun generateMessageBody(msgIndex: Int, groupIndex: Int, rng: SeededRandom): MessageBody {
        val type = rng.nextInt(20)
        return when {
            type < 12 -> MessageBody.Text(textMessages[rng.nextInt(textMessages.size)])
            type < 15 -> {
                val seed = "chat_img_${groupIndex}_$msgIndex"
                val w = listOf(400, 600, 800)[rng.nextInt(3)]
                val h = listOf(300, 400, 600)[rng.nextInt(3)]
                MessageBody.Image(
                    url = "https://picsum.photos/seed/$seed/$w/$h",
                    width = w,
                    height = h,
                    isGif = false
                )
            }
            type < 16 -> {
                val seed = "chat_gif_${groupIndex}_$msgIndex"
                MessageBody.Image(
                    url = "https://picsum.photos/seed/$seed/200/200",
                    width = 200,
                    height = 200,
                    isGif = true
                )
            }
            type < 17 -> {
                val seed = "sticker_${groupIndex}_$msgIndex"
                MessageBody.Sticker(
                    stickerId = seed,
                    url = "https://picsum.photos/seed/$seed/120/120"
                )
            }
            type < 19 -> MessageBody.Voice(
                url = "mock://voice/${groupIndex}_$msgIndex",
                durationMs = (rng.nextInt(55) + 5) * 1000
            )
            else -> MessageBody.Video(
                url = "mock://video/${groupIndex}_$msgIndex",
                thumbnailUrl = "https://picsum.photos/seed/video_${groupIndex}_$msgIndex/320/180",
                durationMs = (rng.nextInt(120) + 10) * 1000
            )
        }
    }

    /** 批量生成压测群（前 14 个复用现有定义，之后动态生成） */
    fun generateStressGroups(count: Int, messagesPerGroup: Int = 0): List<GroupData> =
        (0 until count).map { idx ->
            if (idx < groupDefs.size) {
                val def = groupDefs[idx]
                val members = generateMembers(def.memberCount, idx)
                val messages = if (messagesPerGroup > 0) {
                    generateMessages(def.id, members, count = messagesPerGroup, groupIndex = idx)
                } else emptyList()
                GroupData(id = def.id, name = def.name, members = members, messages = messages)
            } else {
                val id = "stress_g${idx + 1}"
                val name = "压测群 ${idx + 1}"
                val memberCount = 3 + (idx % 10) // 3~12 人
                val members = generateMembers(memberCount, idx)
                val messages = if (messagesPerGroup > 0) {
                    generateMessages(id, members, count = messagesPerGroup, groupIndex = idx)
                } else emptyList()
                GroupData(id = id, name = name, members = members, messages = messages)
            }
        }

    /** 生成单条压测消息（轻量，纯文本，实时时间戳） */
    fun generateStressMessage(conversationId: String, members: List<MockMember>, seq: Long): ChatMessage {
        val sender = members[(seq % members.size).toInt()]
        val base = textMessages[(seq % textMessages.size).toInt()]
        val now = Clock.System.now().toEpochMilliseconds()
        val min = (now / 60_000) % 60
        val ms = now % 1000
        val text = "$base [${min}m${ms}ms]"
        return ChatMessage(
            id = "stress_${conversationId}_$seq",
            conversationId = conversationId,
            senderId = sender.id,
            senderName = sender.name,
            senderAvatar = sender.avatar,
            body = MessageBody.Text(text),
            timestamp = now,
            isMine = false,
            status = SendStatus.SENT
        )
    }

    fun generateAutoReply(conversationId: String, members: List<MockMember>): ChatMessage {
        val rng = SeededRandom(Clock.System.now().toEpochMilliseconds())
        val sender = members[rng.nextInt(members.size)]
        val body = MessageBody.Text(textMessages[rng.nextInt(textMessages.size)])
        return ChatMessage(
            id = "reply_${conversationId}_${Clock.System.now().toEpochMilliseconds()}",
            conversationId = conversationId,
            senderId = sender.id,
            senderName = sender.name,
            senderAvatar = sender.avatar,
            body = body,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            isMine = false,
            status = SendStatus.SENT
        )
    }

    data class GroupDef(val id: String, val name: String, val memberCount: Int)

    data class GroupData(
        val id: String,
        val name: String,
        val members: List<MockMember>,
        val messages: List<ChatMessage>
    )
}

/** Simple deterministic PRNG for repeatable mock data */
class SeededRandom(private var seed: Long) {
    fun nextInt(bound: Int): Int {
        seed = seed * 1103515245 + 12345
        return ((seed / 65536) % 32768).toInt().let { (it and 0x7FFFFFFF) % bound }
    }

    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}
