package com.example.archshowcase.chat.model

import androidx.compose.runtime.Immutable
import com.arkivanov.mvikotlin.core.utils.JvmSerializable

/** 消息窗口快照：DAO 查询结果的不可变切片 */
@Immutable
data class MessageWindow(
    val messages: List<ChatMessage> = emptyList(),
    val hasMoreBefore: Boolean = false,
    val hasMoreAfter: Boolean = false,
    val newMessageCount: Int = 0
) : JvmSerializable {
    companion object {
        const val DEFAULT_WINDOW_SIZE = 200
    }
}
