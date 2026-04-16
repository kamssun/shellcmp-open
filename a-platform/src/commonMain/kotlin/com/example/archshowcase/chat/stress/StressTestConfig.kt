package com.example.archshowcase.chat.stress

/**
 * 聊天压力测试配置。
 *
 * 通过 [AppConfig.chatStressTestConfig] 设置后 build & run，
 * MockChatRepository 会在启动 3s 后自动开始压测。
 */
data class StressTestConfig(
    // ── 数据规模 ──
    /** 总群数（seed 阶段创建） */
    val totalGroups: Int = 500,
    /** 每群种子消息数（0 = 不预填消息，加速 seed） */
    val messagesPerGroup: Int = 0,

    // ── 消息注入 ──
    /** 同时活跃（发消息）的群数 */
    val activeGroups: Int = 200,
    /** 每群每秒消息数 */
    val msgPerSecPerGroup: Double = 2.0,
    /** 压测持续时间 ms */
    val durationMs: Long = 30_000,

    // ── ChatRoom 场景 ──
    /** 压测期间模拟打开的聊天房间 ID（null = 不打开） */
    val openChatRoomId: String? = null,
    /** 该房间额外消息频率（msg/s） */
    val chatRoomMsgPerSec: Double = 50.0,

    // ── 日志 ──
    /** 聚合日志输出间隔 ms */
    val logIntervalMs: Long = 5_000,
) {
    /** 理论总吞吐 msg/s */
    val theoreticalThroughput: Double
        get() = activeGroups * msgPerSecPerGroup + (openChatRoomId?.let { chatRoomMsgPerSec } ?: 0.0)

    companion object {
        /** 场景 2: 稳态消息风暴 — 500 群中 200 个活跃，2 msg/s/群 */
        fun messageStorm() = StressTestConfig(
            totalGroups = 500,
            activeGroups = 200,
            msgPerSecPerGroup = 2.0,
            durationMs = 30_000,
        )

        /** 场景 3: 单群洪峰 + ChatRoom 打开 */
        fun chatRoomFlood() = StressTestConfig(
            totalGroups = 100,
            activeGroups = 50,
            msgPerSecPerGroup = 1.0,
            openChatRoomId = "g1",
            chatRoomMsgPerSec = 50.0,
            durationMs = 20_000,
        )

        /** 场景 4: 背压极限 — 50 群各 100 msg/s = 5000 msg/s */
        fun backpressure() = StressTestConfig(
            totalGroups = 50,
            activeGroups = 50,
            msgPerSecPerGroup = 100.0,
            durationMs = 10_000,
        )

        /** 场景 7: 综合极限 — 1000 群 300 活跃 + ChatRoom */
        fun fullBlast() = StressTestConfig(
            totalGroups = 1000,
            activeGroups = 300,
            msgPerSecPerGroup = 2.0,
            openChatRoomId = "g1",
            chatRoomMsgPerSec = 10.0,
            durationMs = 60_000,
        )
    }
}
