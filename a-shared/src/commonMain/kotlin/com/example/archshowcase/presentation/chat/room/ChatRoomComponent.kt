package com.example.archshowcase.presentation.chat.room

import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.trace.restore.registerScrollRestorableStore
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollUpdateCoordinator
import com.example.archshowcase.chat.model.WindowAnchor
import com.example.archshowcase.presentation.navigation.AppComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ChatRoomComponent {
    val state: StateFlow<ChatRoomStore.State>
    val scrollRestoreEvent: SharedFlow<ScrollPosition>
    val scrollToLatestEvent: SharedFlow<Unit>

    fun onBack()
    fun onSendText(text: String)
    fun onSendImage(url: String, width: Int, height: Int, isGif: Boolean)
    fun onSendSticker(stickerId: String, url: String)
    fun onSendVoice(durationMs: Int)
    fun onMoveWindow(anchor: WindowAnchor)
    fun onResend(messageId: String)
    fun onRecall(messageId: String)
    fun onDelete(messageId: String)
    fun onCopy(messageId: String)
    fun onToggleInputMode()
    fun onToggleEmojiPanel()
    fun onTogglePlusPanel()
    fun updateScrollPosition(firstVisibleIndex: Int, offset: Int)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultChatRoomComponent(
    context: AppComponentContext,
    private val conversationId: String
) : ChatRoomComponent, AppComponentContext by context, KoinComponent {

    init {
        loadChatRoomModule()
    }

    private val storeFactory: ChatRoomStoreFactory by inject()
    private val scrollCoordinator: ScrollUpdateCoordinator by inject()

    private val storeWithScroll = registerScrollRestorableStore(
        name = chatRoomStoreName(conversationId),
        factory = { storeFactory.create(conversationId) },
        getItemCount = { state -> state.messages.size },
        isUserScrolling = scrollCoordinator::isUserScrolling
    )
    private val store = storeWithScroll.first
    override val scrollRestoreEvent: SharedFlow<ScrollPosition> = storeWithScroll.second

    override val state: StateFlow<ChatRoomStore.State> = store.stateFlow

    private val _scrollToLatestEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val scrollToLatestEvent: SharedFlow<Unit> = _scrollToLatestEvent

    private val scope = coroutineScope()

    init {
        store.accept(ChatRoomStore.Intent.Init(conversationId))

        lifecycle.doOnDestroy {
            store.accept(ChatRoomStore.Intent.LeaveConversation)
        }

        scope.launch {
            store.labels.collect { label ->
                when (label) {
                    is ChatRoomStore.Label.CopyToClipboard -> {
                        // Platform clipboard handled by UI layer
                    }
                    is ChatRoomStore.Label.ShowRecallFailed -> {
                        // UI layer handles toast with tr(Res.string.chat_recall_failed)
                    }
                    is ChatRoomStore.Label.ScrollToLatest -> {
                        _scrollToLatestEvent.tryEmit(Unit)
                    }
                }
            }
        }
    }

    override fun onBack() {
        store.accept(ChatRoomStore.Intent.LeaveConversation)
        navigator.pop()
    }

    override fun onSendText(text: String) {
        if (text.isNotBlank()) store.accept(ChatRoomStore.Intent.SendText(text.trim()))
    }

    override fun onSendImage(url: String, width: Int, height: Int, isGif: Boolean) {
        store.accept(ChatRoomStore.Intent.SendImage(url, width, height, isGif))
    }

    override fun onSendSticker(stickerId: String, url: String) {
        store.accept(ChatRoomStore.Intent.SendSticker(stickerId, url))
    }

    override fun onSendVoice(durationMs: Int) {
        store.accept(ChatRoomStore.Intent.SendVoice(durationMs))
    }

    override fun onMoveWindow(anchor: WindowAnchor) {
        store.accept(ChatRoomStore.Intent.MoveWindow(anchor))
    }

    override fun onResend(messageId: String) {
        store.accept(ChatRoomStore.Intent.ResendMessage(messageId))
    }

    override fun onRecall(messageId: String) {
        store.accept(ChatRoomStore.Intent.RecallMessage(messageId))
    }

    override fun onDelete(messageId: String) {
        store.accept(ChatRoomStore.Intent.DeleteMessage(messageId))
    }

    override fun onCopy(messageId: String) {
        store.accept(ChatRoomStore.Intent.CopyMessage(messageId))
    }

    override fun onToggleInputMode() {
        store.accept(ChatRoomStore.Intent.ToggleInputMode)
    }

    override fun onToggleEmojiPanel() {
        store.accept(ChatRoomStore.Intent.ToggleEmojiPanel)
    }

    override fun onTogglePlusPanel() {
        store.accept(ChatRoomStore.Intent.TogglePlusPanel)
    }

    override fun updateScrollPosition(firstVisibleIndex: Int, offset: Int) {
        scrollCoordinator.runWithUserScroll {
            store.accept(ChatRoomStore.Intent.UpdateScrollPosition(firstVisibleIndex, offset))
        }
    }
}
