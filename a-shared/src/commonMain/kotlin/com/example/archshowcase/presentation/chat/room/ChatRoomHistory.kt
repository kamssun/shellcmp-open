package com.example.archshowcase.presentation.chat.room

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.replayable.Replayable
import com.example.archshowcase.chat.local.ChatDao
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.WindowAnchor
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

@Serializable
sealed interface ChatRoomHistoryType : JvmSerializable {
    @Serializable
    data class Initialized(val conversationId: String, val name: String, val memberCount: Int) : ChatRoomHistoryType
    @Serializable
    data class WindowChanged(val anchorTimestamp: Long, val anchorId: String, val windowSize: Int) : ChatRoomHistoryType
    @Serializable
    data class Scroll(val position: ScrollPosition) : ChatRoomHistoryType
    @Serializable
    data class ToggleInputMode(val mode: InputMode) : ChatRoomHistoryType
    @Serializable
    data class ToggleEmojiPanel(val show: Boolean) : ChatRoomHistoryType
    @Serializable
    data class TogglePlusPanel(val show: Boolean) : ChatRoomHistoryType
}

@Replayable(stateClass = ChatRoomStore.State::class)
@Serializable
data class ChatRoomHistoryRecord(
    val type: ChatRoomHistoryType,
    val timestamp: Long
) : JvmSerializable {

    /**
     * State + Record → State。
     * Reducer 始终传入 windowOverride（无 IO）；仅 TTE export/replay 路径不传，
     * 此时通过 ChatDao.queryWindowSync 从本地数据库恢复窗口数据（非主线程）。
     */
    fun applyToState(
        prevState: ChatRoomStore.State,
        windowOverride: MessageWindow? = null
    ): ChatRoomStore.State {
        val newHistory = prevState.appendHistory(this)
        return when (type) {
            is ChatRoomHistoryType.Initialized -> prevState.copy(
                conversationId = type.conversationId,
                conversationName = type.name,
                memberCount = type.memberCount,
                history = newHistory
            )
            is ChatRoomHistoryType.WindowChanged -> {
                val fallback = MessageWindow(
                    messages = prevState.messages,
                    hasMoreBefore = prevState.hasMoreBefore,
                    hasMoreAfter = prevState.hasMoreAfter,
                    newMessageCount = prevState.newMessageCount
                )
                val window = windowOverride ?: resolveWindowFromDb(
                    prevState.conversationId, type, fallback
                )
                prevState.copy(
                    messages = window.messages,
                    hasMoreBefore = window.hasMoreBefore,
                    hasMoreAfter = window.hasMoreAfter,
                    newMessageCount = window.newMessageCount,
                    history = newHistory
                )
            }
            is ChatRoomHistoryType.Scroll -> prevState.copy(
                scrollPosition = type.position,
                history = newHistory
            )
            is ChatRoomHistoryType.ToggleInputMode -> prevState.copy(
                inputMode = type.mode,
                history = newHistory
            )
            is ChatRoomHistoryType.ToggleEmojiPanel -> prevState.copy(
                showEmojiPanel = type.show,
                showPlusPanel = false,
                history = newHistory
            )
            is ChatRoomHistoryType.TogglePlusPanel -> prevState.copy(
                showPlusPanel = type.show,
                showEmojiPanel = false,
                history = newHistory
            )
        }
    }

    fun toIntent(): Any = when (type) {
        is ChatRoomHistoryType.Initialized -> ChatRoomStore.Intent.Init(type.conversationId)
        is ChatRoomHistoryType.WindowChanged -> ChatRoomStore.Intent.MoveWindow(
            if (type.anchorId.isEmpty()) WindowAnchor.Latest
            else WindowAnchor.At(type.anchorTimestamp, type.anchorId)
        )
        is ChatRoomHistoryType.Scroll -> ChatRoomStore.Intent.UpdateScrollPosition(
            type.position.firstVisibleIndex,
            type.position.offset
        )
        is ChatRoomHistoryType.ToggleInputMode -> ChatRoomStore.Intent.ToggleInputMode
        is ChatRoomHistoryType.ToggleEmojiPanel -> ChatRoomStore.Intent.ToggleEmojiPanel
        is ChatRoomHistoryType.TogglePlusPanel -> ChatRoomStore.Intent.TogglePlusPanel
    }
}

private object KoinHelper : KoinComponent

/** TTE 重放时从本地数据库查询窗口数据，查不到则返回 fallback（保留 prevState） */
private fun resolveWindowFromDb(
    conversationId: String,
    type: ChatRoomHistoryType.WindowChanged,
    fallback: MessageWindow
): MessageWindow {
    if (conversationId.isEmpty()) return fallback
    return try {
        val dao: ChatDao = KoinHelper.getKoin().get()
        val anchor = if (type.anchorId.isEmpty()) WindowAnchor.Latest
            else WindowAnchor.At(type.anchorTimestamp, type.anchorId)
        dao.queryWindowSync(conversationId, anchor,
            type.windowSize.coerceAtLeast(MessageWindow.DEFAULT_WINDOW_SIZE))
    } catch (e: Exception) {
        if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        fallback
    }
}
