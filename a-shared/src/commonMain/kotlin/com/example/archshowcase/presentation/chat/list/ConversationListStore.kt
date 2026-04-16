package com.example.archshowcase.presentation.chat.list

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollRestorableState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.chat.model.Conversation

@VfResolvable
interface ConversationListStore : Store<
    ConversationListStore.Intent,
    ConversationListStore.State,
    ConversationListStore.Label
> {

    sealed interface Intent : JvmSerializable {
        data object Refresh : Intent
        data class OpenConversation(val conversationId: String) : Intent
        data class UpdateScrollPosition(val firstVisibleIndex: Int, val offset: Int) : Intent
        data object ScrollToTop : Intent
    }

    @CustomState
    data class State(
        val conversations: List<Conversation> = emptyList(),
        val isLoading: Boolean = true,
        val totalUnread: Int = 0,
        override val scrollPosition: ScrollPosition = ScrollPosition(),
        override val history: AppendOnlyHistory<ConversationListHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<ConversationListHistoryRecord>, ScrollRestorableState {
        override fun hasValidData(): Boolean = conversations.isNotEmpty()
        override fun createInitialState(): ReplayableState<ConversationListHistoryRecord> = State()
    }

    sealed interface Label : JvmSerializable {
        data class NavigateToChat(val conversationId: String) : Label
        data object ScrollToTop : Label
    }

    sealed interface Msg : JvmSerializable {
        data class ConversationsUpdated(
            val conversations: List<Conversation>,
            val conversationsById: Map<String, Conversation>,
            val timestamp: Long
        ) : Msg
        data class TotalUnreadUpdated(val count: Int) : Msg
        data object Loading : Msg
        data class ScrollPositionUpdated(val position: ScrollPosition, val timestamp: Long) : Msg
    }
}
