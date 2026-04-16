package com.example.archshowcase.core.trace.scroll

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.trace.restore.RestorableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private data class ScrollTestState(
    override val scrollPosition: ScrollPosition = ScrollPosition()
) : ScrollRestorableState {
    override fun hasValidData() = true
}

private sealed interface ScrollTestIntent : JvmSerializable {
    data class UpdateScroll(val pos: ScrollPosition) : ScrollTestIntent
}

private sealed interface ScrollTestMsg : JvmSerializable {
    data class ScrollUpdated(val pos: ScrollPosition) : ScrollTestMsg
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScrollRestoreHelperTest {

    private val testDispatcher = StandardTestDispatcher()
    private val storeFactory: StoreFactory = DefaultStoreFactory()

    private fun createStore(): Store<ScrollTestIntent, ScrollTestState, Nothing> =
        storeFactory.create(
            name = "ScrollTestStore",
            initialState = ScrollTestState(),
            executorFactory = {
                object : CoroutineExecutor<ScrollTestIntent, Nothing, ScrollTestState, ScrollTestMsg, Nothing>() {
                    override fun executeIntent(intent: ScrollTestIntent) {
                        when (intent) {
                            is ScrollTestIntent.UpdateScroll ->
                                dispatch(ScrollTestMsg.ScrollUpdated(intent.pos))
                        }
                    }
                }
            },
            reducer = object : Reducer<ScrollTestState, ScrollTestMsg> {
                override fun ScrollTestState.reduce(msg: ScrollTestMsg): ScrollTestState = when (msg) {
                    is ScrollTestMsg.ScrollUpdated -> copy(scrollPosition = msg.pos)
                }
            }
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits scroll restore event when state changes and not user scrolling`() = runTest {
        val store = createStore()
        val scope = TestScope(testDispatcher)
        val helper = ScrollRestoreHelper(scope)

        helper.observe(
            store = store,
            getItemCount = { 10 },
            isUserScrolling = { false }
        )

        store.accept(ScrollTestIntent.UpdateScroll(ScrollPosition(5, 100)))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = helper.scrollRestoreEvent.first()
        assertEquals(ScrollPosition(5, 100), event)

        store.dispose()
    }

    @Test
    fun `does not emit when user is scrolling`() = runTest {
        val store = createStore()
        val scope = TestScope(testDispatcher)
        val helper = ScrollRestoreHelper(scope)

        helper.observe(
            store = store,
            getItemCount = { 10 },
            isUserScrolling = { true }
        )

        store.accept(ScrollTestIntent.UpdateScroll(ScrollPosition(5, 100)))
        testDispatcher.scheduler.advanceUntilIdle()

        store.dispose()
    }

    @Test
    fun `does not emit when item count is zero`() = runTest {
        val store = createStore()
        val scope = TestScope(testDispatcher)
        val helper = ScrollRestoreHelper(scope)

        helper.observe(
            store = store,
            getItemCount = { 0 },
            isUserScrolling = { false }
        )

        store.accept(ScrollTestIntent.UpdateScroll(ScrollPosition(5, 100)))
        testDispatcher.scheduler.advanceUntilIdle()

        store.dispose()
    }

    @Test
    fun `observe with getScrollPosition lambda works`() = runTest {
        val store = createStore()
        val scope = TestScope(testDispatcher)
        val helper = ScrollRestoreHelper(scope)

        helper.observe(
            store = store,
            getScrollPosition = { it.scrollPosition },
            getItemCount = { 10 },
            isUserScrolling = { false }
        )

        store.accept(ScrollTestIntent.UpdateScroll(ScrollPosition(3, 50)))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = helper.scrollRestoreEvent.first()
        assertEquals(ScrollPosition(3, 50), event)

        store.dispose()
    }
}
