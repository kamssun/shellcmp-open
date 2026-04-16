package com.example.archshowcase.presentation.chat.list

import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.trace.restore.registerScrollRestorableStore
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollUpdateCoordinator
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ConversationListComponent {
    val state: StateFlow<ConversationListStore.State>
    val scrollRestoreEvent: SharedFlow<ScrollPosition>
    val scrollToTopEvent: SharedFlow<Unit>
    fun onConversationClick(conversationId: String)
    fun updateScrollPosition(firstVisibleIndex: Int, offset: Int)
    fun scrollToTop()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultConversationListComponent(
    context: AppComponentContext,
) : ConversationListComponent, AppComponentContext by context, KoinComponent {

    init {
        loadConversationListModule()
    }

    private val storeFactory: ConversationListStoreFactory by inject()
    private val scrollCoordinator: ScrollUpdateCoordinator by inject()

    private val storeWithScroll = registerScrollRestorableStore(
        name = CONVERSATION_LIST_STORE_NAME,
        factory = { storeFactory.create() },
        getItemCount = { state -> state.conversations.size },
        isUserScrolling = scrollCoordinator::isUserScrolling
    )
    private val store = storeWithScroll.first
    override val scrollRestoreEvent: SharedFlow<ScrollPosition> = storeWithScroll.second

    override val state: StateFlow<ConversationListStore.State> = store.stateFlow

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent

    private val scope = coroutineScope()

    init {
        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is ConversationListStore.Label.NavigateToChat ->
                        navigator.push(Route.ChatRoom(label.conversationId))
                    is ConversationListStore.Label.ScrollToTop ->
                        _scrollToTopEvent.tryEmit(Unit)
                }
            }
        }
    }

    override fun onConversationClick(conversationId: String) {
        store.accept(ConversationListStore.Intent.OpenConversation(conversationId))
    }

    override fun updateScrollPosition(firstVisibleIndex: Int, offset: Int) {
        scrollCoordinator.runWithUserScroll {
            store.accept(ConversationListStore.Intent.UpdateScrollPosition(firstVisibleIndex, offset))
        }
    }

    override fun scrollToTop() {
        store.accept(ConversationListStore.Intent.ScrollToTop)
    }
}
