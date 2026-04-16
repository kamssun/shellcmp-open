package com.example.archshowcase.presentation.navigation

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.presentation.navigation.NavigationStore.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import com.example.archshowcase.core.AppConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: NavigationStore

    private val factory: NavigationStoreFactory by inject()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                singleOf(::NavigationStoreFactory)
            })
        }
        AppConfig.enableRestore = true
        Dispatchers.setMain(testDispatcher)
        store = factory.create()
    }

    @AfterTest
    fun teardown() {
        store.dispose()
        Dispatchers.resetMain()
        AppConfig.enableRestore = false
        stopKoin()
    }

    @Test
    fun `initial state is Home`() {
        val state = store.state
        assertEquals(Route.Home, state.currentRoute)
        assertEquals(1, state.stackSize)
        assertEquals(listOf(Route.Home), state.stack)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun `Push updates current route and increases stack size`() {
        store.accept(Intent.Push(Route.NetworkDemo))

        val state = store.state
        assertEquals(Route.NetworkDemo, state.currentRoute)
        assertEquals(2, state.stackSize)
        assertEquals(1, state.history.size)
        assertTrue(state.history.first().type is NavHistoryType.Push)
    }

    @Test
    fun `multiple Push accumulates history`() {
        store.accept(Intent.Push(Route.NetworkDemo))
        store.accept(Intent.Push(Route.Detail("123")))
        store.accept(Intent.Push(Route.ImageDemo))

        val state = store.state
        assertEquals(Route.ImageDemo, state.currentRoute)
        assertEquals(4, state.stackSize)
        assertEquals(3, state.history.size)
    }

    @Test
    fun `Pop decreases stack size`() {
        store.accept(Intent.Push(Route.NetworkDemo))
        store.accept(Intent.Push(Route.Detail("123")))
        store.accept(Intent.Pop)

        val state = store.state
        assertEquals(2, state.stackSize)
        assertEquals(3, state.history.size)
        assertTrue(state.history.last().type is NavHistoryType.Pop)
    }

    @Test
    fun `Pop does not go below stack size 1`() {
        store.accept(Intent.Pop)

        val state = store.state
        assertEquals(1, state.stackSize)
    }

    @Test
    fun `BringToFront updates current route`() {
        store.accept(Intent.Push(Route.NetworkDemo))
        store.accept(Intent.BringToFront(Route.CrashDemo))

        val state = store.state
        assertEquals(Route.CrashDemo, state.currentRoute)
        assertTrue(state.history.last().type is NavHistoryType.BringToFront)
    }

    @Test
    fun `ReplaceAll sets new stack`() {
        store.accept(Intent.Push(Route.NetworkDemo))
        store.accept(Intent.ReplaceAll(listOf(Route.Home, Route.CrashDemo)))

        val state = store.state
        assertEquals(Route.CrashDemo, state.currentRoute)
        assertEquals(2, state.stackSize)
        assertTrue(state.history.last().type is NavHistoryType.ReplaceAll)
    }

    @Test
    fun `history records timestamps`() {
        store.accept(Intent.Push(Route.NetworkDemo))

        val record = store.state.history.first()
        assertTrue(record.timestamp > 0)
    }

    @Test
    fun `Detail route records id`() {
        store.accept(Intent.Push(Route.Detail("item-456")))

        val state = store.state
        assertEquals(Route.Detail("item-456"), state.currentRoute)
        val type = state.history.first().type as NavHistoryType.Push
        assertTrue(type.route.contains("item-456"))
    }

    @Test
    fun `history is limited to max size`() {
        repeat(60) { i ->
            store.accept(Intent.Push(Route.Detail("$i")))
        }

        val state = store.state
        assertEquals(60, state.history.size)
    }
}
