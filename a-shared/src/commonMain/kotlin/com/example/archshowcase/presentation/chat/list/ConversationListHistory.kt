package com.example.archshowcase.presentation.chat.list

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.replayable.Replayable
import com.example.archshowcase.chat.model.Conversation
import kotlinx.serialization.Serializable

@Serializable
sealed interface ConversationListHistoryType : JvmSerializable {
    @Serializable
    data class ConversationsUpdated(val conversations: List<Conversation>) : ConversationListHistoryType
    @Serializable
    data class Scroll(val position: ScrollPosition) : ConversationListHistoryType
    @Serializable
    data class OpenConversation(val conversationId: String) : ConversationListHistoryType
}

@Replayable(stateClass = ConversationListStore.State::class)
@Serializable
data class ConversationListHistoryRecord(
    val type: ConversationListHistoryType,
    val timestamp: Long
) : JvmSerializable {

    fun applyToState(prevState: ConversationListStore.State): ConversationListStore.State {
        val newHistory = prevState.appendHistory(this)
        return when (type) {
            is ConversationListHistoryType.ConversationsUpdated -> prevState.copy(
                conversations = type.conversations,
                isLoading = false,
                history = newHistory
            )
            is ConversationListHistoryType.Scroll -> prevState.copy(
                scrollPosition = type.position,
                history = newHistory
            )
            is ConversationListHistoryType.OpenConversation -> prevState.copy(
                history = newHistory
            )
        }
    }

    fun toIntent(): Any = when (type) {
        is ConversationListHistoryType.ConversationsUpdated -> ConversationListStore.Intent.Refresh
        is ConversationListHistoryType.Scroll -> ConversationListStore.Intent.UpdateScrollPosition(
            type.position.firstVisibleIndex,
            type.position.offset
        )
        is ConversationListHistoryType.OpenConversation ->
            ConversationListStore.Intent.OpenConversation(type.conversationId)
    }
}
