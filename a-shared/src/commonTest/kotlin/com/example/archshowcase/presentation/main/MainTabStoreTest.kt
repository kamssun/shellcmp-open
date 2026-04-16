package com.example.archshowcase.presentation.main

import com.example.archshowcase.core.AppConfig
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.presentation.main.MainTabStore.Intent
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainTabStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: MainTabStore

    private val factory: MainTabStoreFactory by inject()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                singleOf(::MainTabStoreFactory)
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
    fun `initial state has selectedIndex 0`() {
        assertEquals(0, store.state.selectedIndex)
        assertTrue(store.state.history.isEmpty())
    }

    @Test
    fun `SelectTab updates selectedIndex`() {
        store.accept(Intent.SelectTab(2))

        assertEquals(2, store.state.selectedIndex)
    }

    @Test
    fun `SelectTab records history`() {
        store.accept(Intent.SelectTab(1))

        assertEquals(1, store.state.history.size)
        assertIs<MainTabHistoryType.SelectTab>(store.state.history.first().type)
        assertEquals(1, (store.state.history.first().type as MainTabHistoryType.SelectTab).index)
    }

    @Test
    fun `SelectTab records timestamp`() {
        store.accept(Intent.SelectTab(3))

        assertTrue(store.state.history.first().timestamp > 0)
    }

    @Test
    fun `multiple SelectTab accumulates history`() {
        store.accept(Intent.SelectTab(1))
        store.accept(Intent.SelectTab(2))
        store.accept(Intent.SelectTab(0))

        assertEquals(3, store.state.history.size)
        assertEquals(0, store.state.selectedIndex)
    }
}
