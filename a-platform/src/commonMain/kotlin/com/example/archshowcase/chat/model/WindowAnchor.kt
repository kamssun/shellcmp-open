package com.example.archshowcase.chat.model

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** 消息窗口锚点：决定 DAO 查询以哪个位置为中心 */
@Serializable
sealed interface WindowAnchor : JvmSerializable {
    /** 粘底模式：查询最新 windowSize 条消息 */
    @Serializable
    data object Latest : WindowAnchor

    /** 定位模式：以 (timestamp, id) 为中心查询前后各 halfWindow 条 */
    @Serializable
    data class At(
        val timestamp: Long,
        val id: String,
        /** 用户离开 Latest 时最新消息的 timestamp，用于计算"真正的新消息数" */
        @Transient val newestSeenTs: Long = 0L,
        @Transient val newestSeenId: String = ""
    ) : WindowAnchor

    companion object {
        /** VF 参数解析：`"Latest"` 或 `"At:timestamp:id"` */
        fun fromString(value: String): WindowAnchor = when {
            value == "Latest" -> Latest
            value.startsWith("At:") -> runCatching {
                val parts = value.split(":", limit = 3)
                At(parts[1].toLong(), parts[2])
            }.getOrDefault(Latest)
            else -> Latest
        }
    }
}
