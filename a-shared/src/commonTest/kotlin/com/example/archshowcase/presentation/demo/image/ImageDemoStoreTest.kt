package com.example.archshowcase.presentation.demo.image

import com.example.archshowcase.core.AppConfig
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.network.api.ImageApi
import com.example.archshowcase.network.api.ImageRepository
import com.example.archshowcase.network.mock.MockImageApi
import com.example.archshowcase.presentation.demo.image.ImageDemoStore.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImageDemoStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: ImageDemoStore

    private val factory: ImageDemoStoreFactory by inject()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<ImageApi> { MockImageApi() }
                singleOf(::ImageRepository)
                singleOf(::ImageDemoStoreFactory)
            })
        }
        AppConfig.useOBOScheduler = false
        AppConfig.enableRestore = true
        Dispatchers.setMain(testDispatcher)
        store = factory.create()
    }

    @AfterTest
    fun teardown() {
        store.dispose()
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        AppConfig.enableRestore = false
        stopKoin()
    }

    @Test
    fun `initial state is empty`() {
        val state = store.state
        assertTrue(state.images.isEmpty())
        assertFalse(state.isInitialLoading)
        assertFalse(state.isLoadingMore)
        assertTrue(state.hasMore)
        assertNull(state.error)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `LoadInitial loads first page`() {
        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isInitialLoading)
        assertEquals(20, state.images.size)  // PAGE_SIZE = 20
        assertEquals(100, state.totalCount)  // MockImageApi.TOTAL_IMAGES = 100
        assertTrue(state.hasMore)
        assertNull(state.error)
    }

    @Test
    fun `LoadInitial ignores when already loaded`() {
        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()
        val firstCount = store.state.images.size

        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(firstCount, store.state.images.size)
    }

    @Test
    fun `LoadMore appends next page`() {
        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertEquals(40, state.images.size)
        assertTrue(state.hasMore)
    }

    @Test
    fun `Reset clears all state`() {
        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.Reset)

        val state = store.state
        assertTrue(state.images.isEmpty())
        assertEquals(0, state.totalCount)
        assertTrue(state.hasMore)
    }

    @Test
    fun `UpdateScrollPosition updates position`() {
        store.accept(Intent.UpdateScrollPosition(firstVisibleIndex = 5, offset = 100))

        val state = store.state
        assertEquals(5, state.scrollPosition.firstVisibleIndex)
        assertEquals(100, state.scrollPosition.offset)
    }

    @Test
    fun `LoadInitial records history`() {
        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.state.history.size)
        assertIs<ImageHistoryType.Load>(store.state.history.first().type)
    }

    @Test
    fun `scroll records history`() {
        store.accept(Intent.UpdateScrollPosition(3, 50))

        assertEquals(1, store.state.history.size)
        assertIs<ImageHistoryType.Scroll>(store.state.history.first().type)
    }

    @Test
    fun `pagination loads all pages`() {
        store.accept(Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        // Load 4 more pages (20 * 5 = 100 total)
        repeat(4) {
            store.accept(Intent.LoadMore)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val state = store.state
        assertEquals(100, state.images.size)
        assertFalse(state.hasMore)
    }

}
