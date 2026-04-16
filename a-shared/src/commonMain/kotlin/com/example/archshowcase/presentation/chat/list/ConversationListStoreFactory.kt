@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.presentation.chat.list

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.scheduler.oboLaunch
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.repository.ChatRepository
import com.example.archshowcase.presentation.chat.list.ConversationListStore.Intent
import com.example.archshowcase.presentation.chat.list.ConversationListStore.Label
import com.example.archshowcase.presentation.chat.list.ConversationListStore.Msg
import com.example.archshowcase.presentation.chat.list.ConversationListStore.State
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

class ConversationListStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val chatRepository: ChatRepository by inject()

    fun create(): ConversationListStore {
        val (initialState, _) = resolveConversationListStoreInitialState()

        return object : ConversationListStore,
            Store<Intent, State, Label> by storeFactory.create(
                name = CONVERSATION_LIST_STORE_NAME,
                initialState = initialState,
                bootstrapper = BootstrapperImpl(chatRepository),
                executorFactory = { ExecutorImpl(chatRepository) },
                reducer = ReducerImpl
            ) {}
    }

    private sealed interface Action {
        data class ConversationsLoaded(
            val conversations: List<Conversation>,
            val conversationsById: Map<String, Conversation>
        ) : Action

        data class TotalUnreadLoaded(val count: Long) : Action
    }

    private class BootstrapperImpl(
        private val chatRepository: ChatRepository
    ) : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.oboLaunch(OBO_TAG) {
                chatRepository.conversations.collectLatest { convs ->
                    val byId = convs.associateBy { it.id }
                    dispatch(Action.ConversationsLoaded(convs, byId))
                }
            }
            scope.oboLaunch(OBO_TAG) {
                chatRepository.totalUnread.collectLatest {
                    dispatch(Action.TotalUnreadLoaded(it))
                }
            }
        }
    }

    private class ExecutorImpl(
        private val chatRepository: ChatRepository
    ) : CoroutineExecutor<Intent, Action, State, Msg, Label>() {

        private fun now() = Clock.System.now().toEpochMilliseconds()

        override fun executeAction(action: Action) {
            when (action) {
                is Action.ConversationsLoaded ->
                    dispatch(Msg.ConversationsUpdated(action.conversations, action.conversationsById, now()))
                is Action.TotalUnreadLoaded ->
                    dispatch(Msg.TotalUnreadUpdated(action.count.toInt()))
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.Refresh -> { /* Data updates via Flow automatically */ }
                is Intent.OpenConversation -> {
                    chatRepository.markRead(intent.conversationId)
                    publish(Label.NavigateToChat(intent.conversationId))
                }
                is Intent.UpdateScrollPosition -> {
                    val position = ScrollPosition(intent.firstVisibleIndex, intent.offset)
                    dispatch(Msg.ScrollPositionUpdated(position, now()))
                }
                is Intent.ScrollToTop -> {
                    publish(Label.ScrollToTop)
                }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {

        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.ConversationsUpdated -> {
                val record = ConversationListHistoryRecord(
                    type = ConversationListHistoryType.ConversationsUpdated(msg.conversations),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
            is Msg.TotalUnreadUpdated -> copy(totalUnread = msg.count)
            is Msg.Loading -> copy(isLoading = true)
            is Msg.ScrollPositionUpdated -> {
                val record = ConversationListHistoryRecord(
                    type = ConversationListHistoryType.Scroll(msg.position),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
        }
    }

    companion object {
        private const val OBO_TAG = "ConvList"
    }
}
