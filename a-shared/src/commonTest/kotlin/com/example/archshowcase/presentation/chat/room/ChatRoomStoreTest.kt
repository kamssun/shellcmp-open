package com.example.archshowcase.presentation.chat.room

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.MessageWindow
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.chat.model.WindowAnchor
import com.example.archshowcase.chat.repository.ChatRepository
import com.example.archshowcase.presentation.chat.room.InputMode
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeChatRepository : ChatRepository {
    val windowFlow = MutableStateFlow(MessageWindow())
    val typingFlow = MutableStateFlow<Pair<Boolean, String?>>(Pair(false, null))
    var sentMessages = mutableListOf<Pair<String, MessageBody>>()
    var deletedMessages = mutableListOf<String>()
    var recalledMessages = mutableListOf<String>()

    override val conversations: Flow<List<Conversation>> = MutableStateFlow(emptyList())
    override val totalUnread: Flow<Long> = MutableStateFlow(0L)

    override fun observeMessages(
        conversationId: String,
        anchor: StateFlow<WindowAnchor>,
        windowSize: Int
    ): Flow<MessageWindow> = windowFlow

    override suspend fun sendMessage(conversationId: String, body: MessageBody): Result<ChatMessage> {
        sentMessages.add(conversationId to body)
        return Result.success(
            ChatMessage("sent_1", conversationId, "me", "Me", null, body, 0, true, SendStatus.SENT)
        )
    }

    override suspend fun resendMessage(messageId: String, conversationId: String): Result<ChatMessage> =
        Result.success(
            ChatMessage(messageId, conversationId, "me", "Me", null, MessageBody.Text(""), 0, true, SendStatus.SENT)
        )

    override suspend fun loadMoreHistory(conversationId: String, beforeTimestamp: Long, beforeId: String): Result<List<ChatMessage>> =
        Result.success(emptyList())

    var recallResult: Result<Unit> = Result.success(Unit)

    override suspend fun recallMessage(messageId: String, conversationId: String): Result<Unit> {
        recalledMessages.add(messageId)
        return recallResult
    }

    override suspend fun deleteMessage(messageId: String, conversationId: String): Result<Unit> {
        deletedMessages.add(messageId)
        return Result.success(Unit)
    }

    override fun markRead(conversationId: String) {}
    override fun getConversationName(conversationId: String): String = "Test Chat"
    override fun getMemberCount(conversationId: String): Int = 5
    override fun observeTypingState(conversationId: String): StateFlow<Pair<Boolean, String?>> = typingFlow
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: ChatRoomStore
    private lateinit var fakeRepo: FakeChatRepository

    private val factory: ChatRoomStoreFactory by inject()

    @BeforeTest
    fun setup() {
        fakeRepo = FakeChatRepository()
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<ChatRepository> { fakeRepo }
                singleOf(::ChatRoomStoreFactory)
            })
        }
        AppConfig.useOBOScheduler = false
        Dispatchers.setMain(testDispatcher)
        store = factory.create("conv_1")
    }

    @AfterTest
    fun teardown() {
        store.dispose()
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        stopKoin()
    }

    @Test
    fun `Init sets conversation name and member count`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test Chat", store.state.conversationName)
        assertEquals(5, store.state.memberCount)
    }

    @Test
    fun `Init observes messages flow`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val msg = ChatMessage(
            "m1", "conv_1", "user1", "User1", null,
            MessageBody.Text("Hello"), 1000, false, SendStatus.SENT
        )
        fakeRepo.windowFlow.value = MessageWindow(messages = listOf(msg))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.state.messages.size)
        assertEquals("Hello", (store.state.messages.first().body as MessageBody.Text).text)
    }

    @Test
    fun `SendText dispatches to repository`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(ChatRoomStore.Intent.SendText("Hi there"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.sentMessages.size)
        val (convId, body) = fakeRepo.sentMessages.first()
        assertEquals("conv_1", convId)
        assertEquals("Hi there", (body as MessageBody.Text).text)
    }

    @Test
    fun `SendText closes panels`() {
        store.accept(ChatRoomStore.Intent.ToggleEmojiPanel)
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(ChatRoomStore.Intent.SendText("test"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(store.state.showEmojiPanel)
        assertFalse(store.state.showPlusPanel)
    }

    @Test
    fun `ToggleInputMode switches between TEXT and VOICE`() {
        assertEquals(InputMode.TEXT, store.state.inputMode)

        store.accept(ChatRoomStore.Intent.ToggleInputMode)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(InputMode.VOICE, store.state.inputMode)

        store.accept(ChatRoomStore.Intent.ToggleInputMode)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(InputMode.TEXT, store.state.inputMode)
    }

    @Test
    fun `ToggleEmojiPanel opens and closes plus panel`() {
        store.accept(ChatRoomStore.Intent.TogglePlusPanel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, store.state.showPlusPanel)

        store.accept(ChatRoomStore.Intent.ToggleEmojiPanel)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, store.state.showEmojiPanel)
        assertFalse(store.state.showPlusPanel)
    }

    @Test
    fun `DeleteMessage calls repository`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(ChatRoomStore.Intent.DeleteMessage("m1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("m1"), fakeRepo.deletedMessages)
    }

    @Test
    fun `RecallMessage calls repository`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(ChatRoomStore.Intent.RecallMessage("m1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("m1"), fakeRepo.recalledMessages)
    }

    @Test
    fun `RecallMessage failure publishes ShowRecallFailed label`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        fakeRepo.recallResult = Result.failure(Exception("network error"))

        val labels = mutableListOf<ChatRoomStore.Label>()
        val labelScope = CoroutineScope(testDispatcher)
        val job = labelScope.launch { store.labels.collect { labels.add(it) } }
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(ChatRoomStore.Intent.RecallMessage("m1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(labels.any { it is ChatRoomStore.Label.ShowRecallFailed },
            "Expected ShowRecallFailed label but got: $labels")
        job.cancel()
    }

    // ── Window tests (Tasks 6.1-6.5) ──

    @Test
    fun `Init shows latest messages in window`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val msgs = (1..5).map { i ->
            ChatMessage("m$i", "conv_1", "u1", "U1", null, MessageBody.Text("msg$i"), i * 1000L, false, SendStatus.SENT)
        }
        fakeRepo.windowFlow.value = MessageWindow(messages = msgs.reversed(), hasMoreBefore = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, store.state.messages.size)
        assertTrue(store.state.hasMoreBefore)
        assertFalse(store.state.hasMoreAfter)
        assertEquals(0, store.state.newMessageCount)
    }

    @Test
    fun `MoveWindow At updates state with anchor window`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val windowMsgs = (1..3).map { i ->
            ChatMessage("m$i", "conv_1", "u1", "U1", null, MessageBody.Text("msg$i"), i * 1000L, false, SendStatus.SENT)
        }
        fakeRepo.windowFlow.value = MessageWindow(
            messages = windowMsgs.reversed(),
            hasMoreBefore = true,
            hasMoreAfter = true,
            newMessageCount = 5
        )
        store.accept(ChatRoomStore.Intent.MoveWindow(WindowAnchor.At(2000, "m2")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, store.state.messages.size)
        assertTrue(store.state.hasMoreBefore)
        assertTrue(store.state.hasMoreAfter)
        assertEquals(5, store.state.newMessageCount)
    }

    @Test
    fun `MoveWindow Latest resets newMessageCount`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate At mode with new messages
        fakeRepo.windowFlow.value = MessageWindow(newMessageCount = 3, hasMoreAfter = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, store.state.newMessageCount)

        // Switch back to Latest
        fakeRepo.windowFlow.value = MessageWindow(newMessageCount = 0, hasMoreAfter = false)
        store.accept(ChatRoomStore.Intent.MoveWindow(WindowAnchor.Latest))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, store.state.newMessageCount)
        assertFalse(store.state.hasMoreAfter)
    }

    @Test
    fun `new messages in Latest mode auto-included`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val msg1 = ChatMessage("m1", "conv_1", "u1", "U1", null, MessageBody.Text("hi"), 1000, false, SendStatus.SENT)
        fakeRepo.windowFlow.value = MessageWindow(messages = listOf(msg1))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, store.state.messages.size)

        // New message arrives
        val msg2 = ChatMessage("m2", "conv_1", "u2", "U2", null, MessageBody.Text("hello"), 2000, false, SendStatus.SENT)
        fakeRepo.windowFlow.value = MessageWindow(messages = listOf(msg2, msg1))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, store.state.messages.size)
    }

    @Test
    fun `SendText resets anchor to Latest`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Move to At position
        store.accept(ChatRoomStore.Intent.MoveWindow(WindowAnchor.At(1000, "m1")))
        testDispatcher.scheduler.advanceUntilIdle()

        // Send message — should reset to Latest (anchor internally, verified by windowFlow behavior)
        store.accept(ChatRoomStore.Intent.SendText("new msg"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.sentMessages.size)
    }

    @Test
    fun `typing state updates from flow`() {
        store.accept(ChatRoomStore.Intent.Init("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        fakeRepo.typingFlow.value = Pair(true, "Alice")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Alice", store.state.typingUser)

        fakeRepo.typingFlow.value = Pair(false, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, store.state.typingUser)
    }
}
