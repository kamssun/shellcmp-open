package com.example.archshowcase.presentation.chat.room

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollRestorableState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.WindowAnchor

enum class InputMode : JvmSerializable { TEXT, VOICE }

@VfResolvable
interface ChatRoomStore : Store<
    ChatRoomStore.Intent,
    ChatRoomStore.State,
    ChatRoomStore.Label
> {

    sealed interface Intent : JvmSerializable {
        data class Init(val conversationId: String) : Intent
        data class SendText(val text: String) : Intent
        data class SendImage(val url: String, val width: Int, val height: Int, val isGif: Boolean) : Intent
        data class SendSticker(val stickerId: String, val url: String) : Intent
        data class SendVoice(val durationMs: Int) : Intent
        data class MoveWindow(val anchor: WindowAnchor) : Intent
        data class ResendMessage(val messageId: String) : Intent
        data class RecallMessage(val messageId: String) : Intent
        data class DeleteMessage(val messageId: String) : Intent
        data class CopyMessage(val messageId: String) : Intent
        data object ToggleInputMode : Intent
        data object ToggleEmojiPanel : Intent
        data object TogglePlusPanel : Intent
        data object LeaveConversation : Intent
        data class UpdateScrollPosition(val firstVisibleIndex: Int, val offset: Int) : Intent
    }

    @CustomState
    data class State(
        val conversationId: String = "",
        val conversationName: String = "",
        val memberCount: Int = 0,
        val messages: List<ChatMessage> = emptyList(),
        val hasMoreBefore: Boolean = true,
        val hasMoreAfter: Boolean = false,
        val newMessageCount: Int = 0,
        val inputMode: InputMode = InputMode.TEXT,
        val showEmojiPanel: Boolean = false,
        val showPlusPanel: Boolean = false,
        val typingUser: String? = null,
        override val scrollPosition: ScrollPosition = ScrollPosition(),
        override val history: AppendOnlyHistory<ChatRoomHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<ChatRoomHistoryRecord>, ScrollRestorableState {
        override fun hasValidData(): Boolean = conversationId.isNotEmpty()
        override fun createInitialState(): ReplayableState<ChatRoomHistoryRecord> = State()
    }

    sealed interface Label : JvmSerializable {
        data class CopyToClipboard(val text: String) : Label
        data object ShowRecallFailed : Label
        data object ScrollToLatest : Label
    }

    sealed interface Msg : JvmSerializable {
        data class Initialized(
            val conversationId: String,
            val name: String,
            val memberCount: Int,
            val timestamp: Long
        ) : Msg
        data class WindowUpdated(val window: MessageWindow, val timestamp: Long) : Msg
        data class TypingStateUpdated(val typingUser: String?, val timestamp: Long) : Msg
        data class InputModeChanged(val mode: InputMode, val timestamp: Long) : Msg
        data class EmojiPanelToggled(val show: Boolean, val timestamp: Long) : Msg
        data class PlusPanelToggled(val show: Boolean, val timestamp: Long) : Msg
        data class ScrollPositionUpdated(val position: ScrollPosition, val timestamp: Long) : Msg
    }
}
