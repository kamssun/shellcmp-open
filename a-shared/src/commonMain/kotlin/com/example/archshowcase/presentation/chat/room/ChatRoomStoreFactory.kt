@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.presentation.chat.room

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.scheduler.oboLaunch
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.WindowAnchor
import com.example.archshowcase.chat.repository.ChatRepository
import com.example.archshowcase.presentation.chat.room.ChatRoomStore.Intent
import com.example.archshowcase.presentation.chat.room.ChatRoomStore.Label
import com.example.archshowcase.presentation.chat.room.ChatRoomStore.Msg
import com.example.archshowcase.presentation.chat.room.ChatRoomStore.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.concurrent.Volatile
import kotlin.random.Random
import kotlin.time.Clock

class ChatRoomStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val chatRepository: ChatRepository by inject()

    fun create(conversationId: String): ChatRoomStore {
        val instanceName = chatRoomStoreName(conversationId)
        val restored = RestoreRegistry.resolveInitialState<State>(instanceName)
        val initialState = restored ?: State()

        return object : ChatRoomStore,
            Store<Intent, State, Label> by storeFactory.create(
                name = instanceName,
                initialState = initialState,
                executorFactory = { ExecutorImpl(chatRepository, conversationId) },
                reducer = ReducerImpl
            ) {}
    }

    private class ExecutorImpl(
        private val repo: ChatRepository,
        private val conversationId: String
    ) : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {

        private val anchorFlow = MutableStateFlow<WindowAnchor>(WindowAnchor.Latest)
        /** 用户在 Latest 模式下看到的最新消息位置，离开底部后冻结 */
        @Volatile
        private var newestSeen = SeenPosition(0L, "")

        private fun now() = Clock.System.now().toEpochMilliseconds()

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.Init -> init()
                is Intent.SendText -> sendMessage(MessageBody.Text(intent.text))
                is Intent.SendImage -> {
                    val url = intent.url.ifEmpty {
                        "https://picsum.photos/seed/img_${now()}/${intent.width}/${intent.height}"
                    }
                    sendMessage(MessageBody.Image(url, intent.width, intent.height, intent.isGif))
                }
                is Intent.SendSticker -> sendMessage(
                    MessageBody.Sticker(intent.stickerId, intent.url)
                )
                is Intent.SendVoice -> {
                    val duration = if (intent.durationMs > 0) intent.durationMs
                        else Random.nextInt(3, 30) * 1000
                    sendMessage(
                        MessageBody.Voice("mock://voice/${now()}", duration)
                    )
                }
                is Intent.MoveWindow -> {
                    val seen = newestSeen
                    val anchor = when (val a = intent.anchor) {
                        is WindowAnchor.Latest -> a
                        is WindowAnchor.At -> a.copy(
                            newestSeenTs = seen.ts,
                            newestSeenId = seen.id
                        )
                    }
                    anchorFlow.value = anchor
                }
                is Intent.ResendMessage -> resend(intent.messageId)
                is Intent.RecallMessage -> recall(intent.messageId)
                is Intent.DeleteMessage -> delete(intent.messageId)
                is Intent.CopyMessage -> copy(intent.messageId)
                is Intent.ToggleInputMode -> {
                    val current = state().inputMode
                    val next = if (current == InputMode.TEXT) InputMode.VOICE else InputMode.TEXT
                    dispatch(Msg.InputModeChanged(next, now()))
                }
                is Intent.ToggleEmojiPanel -> dispatch(Msg.EmojiPanelToggled(!state().showEmojiPanel, now()))
                is Intent.TogglePlusPanel -> dispatch(Msg.PlusPanelToggled(!state().showPlusPanel, now()))
                is Intent.LeaveConversation -> repo.onLeaveConversation()
                is Intent.UpdateScrollPosition -> {
                    val position = ScrollPosition(intent.firstVisibleIndex, intent.offset)
                    dispatch(Msg.ScrollPositionUpdated(position, now()))
                }
            }
        }

        private fun init() {
            if (state().conversationId.isNotEmpty()) return // restored state, skip init
            val name = repo.getConversationName(conversationId) ?: ""
            val count = repo.getMemberCount(conversationId)
            dispatch(Msg.Initialized(conversationId, name, count, now()))

            repo.markRead(conversationId)

            scope.oboLaunch(OBO_TAG) {
                repo.observeMessages(conversationId, anchorFlow).collectLatest { window ->
                    // Latest 模式下持续更新 newestSeen，At 模式下冻结
                    if (anchorFlow.value is WindowAnchor.Latest) {
                        window.messages.firstOrNull()?.let {
                            newestSeen = SeenPosition(it.timestamp, it.id)
                        }
                    }
                    dispatch(Msg.WindowUpdated(window, now()))
                }
            }
            scope.oboLaunch(OBO_TAG) {
                repo.observeTypingState(conversationId).collectLatest { (isTyping, name) ->
                    dispatch(Msg.TypingStateUpdated(if (isTyping) name else null, now()))
                }
            }
        }

        private fun sendMessage(body: MessageBody) {
            // Close panels on send + reset anchor to Latest
            dispatch(Msg.EmojiPanelToggled(false, now()))
            dispatch(Msg.PlusPanelToggled(false, now()))
            anchorFlow.value = WindowAnchor.Latest
            publish(Label.ScrollToLatest)
            scope.oboLaunch(OBO_TAG) {
                repo.sendMessage(conversationId, body)
            }
        }

        private fun resend(messageId: String) {
            scope.oboLaunch(OBO_TAG) { repo.resendMessage(messageId, conversationId) }
        }

        private fun recall(messageId: String) {
            scope.oboLaunch(OBO_TAG) {
                repo.recallMessage(messageId, conversationId)
                    .onFailure { publish(Label.ShowRecallFailed) }
            }
        }

        private fun delete(messageId: String) {
            scope.oboLaunch(OBO_TAG) { repo.deleteMessage(messageId, conversationId) }
        }

        private fun copy(messageId: String) {
            val msg = state().messages.find { it.id == messageId } ?: return
            val text = when (val body = msg.body) {
                is MessageBody.Text -> body.text
                else -> return
            }
            publish(Label.CopyToClipboard(text))
        }

        companion object {
            private const val OBO_TAG = "ChatRoom"
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.Initialized -> {
                val record = ChatRoomHistoryRecord(
                    type = ChatRoomHistoryType.Initialized(msg.conversationId, msg.name, msg.memberCount),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
            is Msg.WindowUpdated -> {
                val window = msg.window
                // Latest mode: anchor 为空（applyToState 将识别为 Latest）
                // At mode: 记录窗口中心的消息作为 anchor
                val first = window.messages.firstOrNull()
                val record = ChatRoomHistoryRecord(
                    type = ChatRoomHistoryType.WindowChanged(
                        anchorTimestamp = first?.timestamp ?: 0L,
                        // 空串 = Latest（见 ChatRoomHistory.toIntent 中 anchorId.isEmpty() → WindowAnchor.Latest）
                        anchorId = if (window.hasMoreAfter || window.newMessageCount > 0) first?.id ?: "" else "",
                        windowSize = window.messages.size
                    ),
                    timestamp = msg.timestamp
                )
                record.applyToState(this, window)
            }
            // typing 是瞬态，不进 history，不参与 TTE replay
            is Msg.TypingStateUpdated -> copy(typingUser = msg.typingUser)
            is Msg.InputModeChanged -> {
                val record = ChatRoomHistoryRecord(
                    type = ChatRoomHistoryType.ToggleInputMode(msg.mode),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
            is Msg.EmojiPanelToggled -> {
                val record = ChatRoomHistoryRecord(
                    type = ChatRoomHistoryType.ToggleEmojiPanel(msg.show),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
            is Msg.PlusPanelToggled -> {
                val record = ChatRoomHistoryRecord(
                    type = ChatRoomHistoryType.TogglePlusPanel(msg.show),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
            is Msg.ScrollPositionUpdated -> {
                val record = ChatRoomHistoryRecord(
                    type = ChatRoomHistoryType.Scroll(msg.position),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
        }
    }
}

/** 用户在 Latest 模式下看到的最新消息位置快照，单次赋值保证原子性 */
private data class SeenPosition(val ts: Long, val id: String)

/** 生成实例化 store name，每个会话独占 RestoreRegistry 槽位 */
fun chatRoomStoreName(conversationId: String) = "$CHAT_ROOM_STORE_NAME:$conversationId"
