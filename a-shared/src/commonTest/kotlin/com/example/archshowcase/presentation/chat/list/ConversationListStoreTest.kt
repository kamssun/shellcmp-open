package com.example.archshowcase.presentation.chat.list

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import kotlin.test.assertTrue

private class FakeChatRepository : ChatRepository {
    val conversationsFlow = MutableStateFlow(
        listOf(
            Conversation(
                id = "conv_1",
                name = "Test Group",
                memberAvatars = listOf("avatar1"),
                memberCount = 3,
                lastActiveTime = 1000L,
                unreadCount = 2
            )
        )
    )
    val totalUnreadFlow = MutableStateFlow(2L)
    var lastMarkedReadId: String? = null

    override val conversations: Flow<List<Conversation>> = conversationsFlow
    override val totalUnread: Flow<Long> = totalUnreadFlow

    override fun observeMessages(
        conversationId: String,
        anchor: StateFlow<WindowAnchor>,
        windowSize: Int
    ): Flow<MessageWindow> = MutableStateFlow(MessageWindow())

    override suspend fun sendMessage(conversationId: String, body: MessageBody): Result<ChatMessage> =
        Result.success(
            ChatMessage("m1", conversationId, "me", "Me", null, body, 0, true, SendStatus.SENT)
        )

    override suspend fun resendMessage(messageId: String, conversationId: String): Result<ChatMessage> =
        Result.success(
            ChatMessage(messageId, conversationId, "", "", null, MessageBody.Text(""), 0, true, SendStatus.SENT)
        )

    override suspend fun loadMoreHistory(conversationId: String, beforeTimestamp: Long, beforeId: String): Result<List<ChatMessage>> =
        Result.success(emptyList())

    override suspend fun recallMessage(messageId: String, conversationId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteMessage(messageId: String, conversationId: String): Result<Unit> = Result.success(Unit)

    override fun markRead(conversationId: String) {
        lastMarkedReadId = conversationId
    }

    override fun getConversationName(conversationId: String): String? = "Test Group"
    override fun getMemberCount(conversationId: String): Int = 3

    override fun observeTypingState(conversationId: String): StateFlow<Pair<Boolean, String?>> =
        MutableStateFlow(Pair(false, null))
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: ConversationListStore
    private lateinit var fakeChatRepo: FakeChatRepository

    private val factory: ConversationListStoreFactory by inject()

    @BeforeTest
    fun setup() {
        fakeChatRepo = FakeChatRepository()
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<ChatRepository> { fakeChatRepo }
                singleOf(::ConversationListStoreFactory)
            })
        }
        AppConfig.useOBOScheduler = false
        Dispatchers.setMain(testDispatcher)
        store = factory.create()
    }

    @AfterTest
    fun teardown() {
        store.dispose()
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        stopKoin()
    }

    @Test
    fun `initial state is loading`() {
        assertEquals(true, store.state.isLoading)
        assertTrue(store.state.conversations.isEmpty())
    }

    @Test
    fun `bootstrapper loads conversations`() {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.state.conversations.size)
        assertEquals("conv_1", store.state.conversations.first().id)
        assertEquals(false, store.state.isLoading)
    }

    @Test
    fun `bootstrapper loads total unread count`() {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, store.state.totalUnread)
    }

    @Test
    fun `OpenConversation marks conversation read`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(ConversationListStore.Intent.OpenConversation("conv_1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("conv_1", fakeChatRepo.lastMarkedReadId)
    }

    @Test
    fun `conversations flow update refreshes state`() {
        testDispatcher.scheduler.advanceUntilIdle()

        fakeChatRepo.conversationsFlow.value = listOf(
            Conversation("conv_1", "G1", emptyList(), 2, 1000, 0),
            Conversation("conv_2", "G2", emptyList(), 3, 2000, 1)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, store.state.conversations.size)
    }
}
