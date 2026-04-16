package com.example.archshowcase.presentation.demo.obo

import com.example.archshowcase.core.AppConfig
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.data.settings.SettingsRepository
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeSettingsRepository : SettingsRepository {
    private val _useOBOScheduler = MutableStateFlow(true)
    override val useOBOScheduler: Flow<Boolean> = _useOBOScheduler
    override suspend fun setUseOBOScheduler(enabled: Boolean) {
        _useOBOScheduler.value = enabled
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OBODemoStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: OBODemoStore

    private val factory: OBODemoStoreFactory by inject()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<SettingsRepository> { FakeSettingsRepository() }
                singleOf(::OBODemoStoreFactory)
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
    fun `initial state has defaults`() {
        val state = store.state
        assertEquals(10, state.effectsPerItem)
        assertEquals(3, state.blockTimeMs)
        assertTrue(state.useOBO)
        assertEquals(0, state.reloadTrigger)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun `bootstrapper loads OBO setting`() {
        testDispatcher.scheduler.advanceUntilIdle()

        // InMemorySettingsRepository defaults to true
        assertTrue(store.state.useOBO)
    }

    @Test
    fun `SetEffectsPerItem updates effects count`() {
        store.accept(Intent.SetEffectsPerItem(20))

        assertEquals(20, store.state.effectsPerItem)
        assertEquals(1, store.state.history.size)
        assertIs<OBOHistoryType.SetEffects>(store.state.history.first().type)
    }

    @Test
    fun `SetBlockTime updates block time`() {
        store.accept(Intent.SetBlockTime(10))

        assertEquals(10, store.state.blockTimeMs)
        assertEquals(1, store.state.history.size)
        assertIs<OBOHistoryType.SetBlockTime>(store.state.history.first().type)
    }

    @Test
    fun `Reload increments trigger`() {
        store.accept(Intent.Reload)

        assertEquals(1, store.state.reloadTrigger)
        assertIs<OBOHistoryType.Reload>(store.state.history.first().type)
    }

    @Test
    fun `multiple Reload increments trigger each time`() {
        repeat(3) { store.accept(Intent.Reload) }

        assertEquals(3, store.state.reloadTrigger)
        assertEquals(3, store.state.history.size)
    }

    @Test
    fun `ToggleOBO disables OBO`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.ToggleOBO(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, store.state.useOBO)
        assertIs<OBOHistoryType.ToggleOBO>(store.state.history.last().type)
    }

    @Test
    fun `UpdateScrollPosition records position`() {
        store.accept(Intent.UpdateScrollPosition(firstVisibleIndex = 7, offset = 200))

        val state = store.state
        assertEquals(7, state.scrollPosition.firstVisibleIndex)
        assertEquals(200, state.scrollPosition.offset)
        assertIs<OBOHistoryType.Scroll>(state.history.first().type)
    }

    @Test
    fun `history accumulates across intents`() {
        store.accept(Intent.SetEffectsPerItem(5))
        store.accept(Intent.SetBlockTime(1))
        store.accept(Intent.Reload)
        store.accept(Intent.UpdateScrollPosition(2, 50))

        assertEquals(4, store.state.history.size)
    }

    @Test
    fun `history records timestamps`() {
        store.accept(Intent.SetEffectsPerItem(15))

        assertTrue(store.state.history.first().timestamp > 0)
    }
}
