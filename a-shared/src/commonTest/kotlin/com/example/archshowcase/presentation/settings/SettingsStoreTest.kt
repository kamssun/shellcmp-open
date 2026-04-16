package com.example.archshowcase.presentation.settings

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.data.settings.SettingsRepository
import com.example.archshowcase.presentation.settings.SettingsStore.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.archshowcase.core.AppConfig
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
import kotlin.test.assertTrue

private class FakeSettingsRepository : SettingsRepository {
    private val _useOBOScheduler = MutableStateFlow(true)
    override val useOBOScheduler: Flow<Boolean> = _useOBOScheduler
    override suspend fun setUseOBOScheduler(enabled: Boolean) {
        _useOBOScheduler.value = enabled
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: SettingsStore

    private val factory: SettingsStoreFactory by inject()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<SettingsRepository> { FakeSettingsRepository() }
                singleOf(::SettingsStoreFactory)
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
    fun `initial state has OBO enabled`() {
        assertEquals(true, store.state.useOBOScheduler)
        assertTrue(store.state.history.isEmpty())
    }

    @Test
    fun `bootstrapper loads settings from repository`() {
        testDispatcher.scheduler.advanceUntilIdle()

        // InMemorySettingsRepository defaults to true
        assertEquals(true, store.state.useOBOScheduler)
    }

    @Test
    fun `SetOBOScheduler disables OBO`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.SetOBOScheduler(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, store.state.useOBOScheduler)
        assertEquals(1, store.state.history.size)
    }

    @Test
    fun `SetOBOScheduler enables OBO`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.SetOBOScheduler(false))
        testDispatcher.scheduler.advanceUntilIdle()
        store.accept(Intent.SetOBOScheduler(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, store.state.useOBOScheduler)
        assertEquals(2, store.state.history.size)
    }

    @Test
    fun `history records toggle type`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.SetOBOScheduler(false))
        testDispatcher.scheduler.advanceUntilIdle()

        val record = store.state.history.first()
        val type = record.type
        assertTrue(type is SettingsHistoryType.SetOBOScheduler)
        assertEquals(false, (type as SettingsHistoryType.SetOBOScheduler).enabled)
    }

    @Test
    fun `history records timestamp`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.SetOBOScheduler(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(store.state.history.first().timestamp > 0)
    }

    @Test
    fun `multiple toggles accumulate history`() {
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.SetOBOScheduler(false))
        testDispatcher.scheduler.advanceUntilIdle()
        store.accept(Intent.SetOBOScheduler(true))
        testDispatcher.scheduler.advanceUntilIdle()
        store.accept(Intent.SetOBOScheduler(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, store.state.history.size)
        assertEquals(false, store.state.useOBOScheduler)
    }
}
