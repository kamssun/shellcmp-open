package com.example.archshowcase.presentation.demo

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.scroll.ScrollUpdateCoordinator
import com.example.archshowcase.data.settings.SettingsRepository
import com.example.archshowcase.network.api.ImageApi
import com.example.archshowcase.network.api.ImageRepository
import com.example.archshowcase.network.api.UserApi
import com.example.archshowcase.network.api.UserRepository
import com.example.archshowcase.network.mock.MockImageApi
import com.example.archshowcase.network.mock.MockUserApi
import com.example.archshowcase.presentation.demo.image.DefaultImageDemoComponent
import com.example.archshowcase.presentation.demo.image.ImageDemoStoreFactory
import com.example.archshowcase.presentation.demo.network.DefaultNetworkDemoComponent
import com.example.archshowcase.presentation.demo.network.NetworkDemoStoreFactory
import com.example.archshowcase.presentation.demo.obo.DefaultOBODemoComponent
import com.example.archshowcase.presentation.demo.obo.OBODemoStoreFactory
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.DefaultAppComponentContext
import com.example.archshowcase.presentation.navigation.Navigator
import com.example.archshowcase.presentation.navigation.Route
import com.example.archshowcase.presentation.settings.DefaultSettingsComponent
import com.example.archshowcase.presentation.settings.SettingsStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeSettingsRepository : SettingsRepository {
    private val _useOBOScheduler = MutableStateFlow(true)
    override val useOBOScheduler: Flow<Boolean> = _useOBOScheduler
    override suspend fun setUseOBOScheduler(enabled: Boolean) {
        _useOBOScheduler.value = enabled
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ComponentWithStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private val lifecycle = LifecycleRegistry()
    private lateinit var appContext: AppComponentContext

    private val navigatedRoutes = mutableListOf<Route>()
    private val poppedCount = mutableListOf<Unit>()
    private val fakeNavigator = object : Navigator {
        override val currentRoute: Route = Route.Home
        override fun push(route: Route) { navigatedRoutes.add(route) }
        override fun pop() { poppedCount.add(Unit) }
        override fun replaceAll(vararg routes: Route) {}
        override fun restore() {}
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppConfig.enableRestore = true
        AppConfig.useOBOScheduler = false
        RestoreRegistry.clear()
        val baseContext = DefaultComponentContext(lifecycle = lifecycle)
        appContext = DefaultAppComponentContext(baseContext, fakeNavigator)

        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<SettingsRepository> { FakeSettingsRepository() }
                single<UserApi> { MockUserApi() }
                single<ImageApi> { MockImageApi() }
                single { UserRepository() }
                single { ImageRepository() }
                single { ScrollUpdateCoordinator() }
                single { OBODemoStoreFactory() }
                single { NetworkDemoStoreFactory() }
                single { ImageDemoStoreFactory() }
                single { SettingsStoreFactory() }
            })
        }
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        RestoreRegistry.clear()
        stopKoin()
    }

    // --- OBODemoComponent ---

    @Test
    fun `OBODemoComponent can be created and has state`() {
        val component = DefaultOBODemoComponent(appContext)
        assertNotNull(component.state.value)
        assertEquals(10, component.state.value.effectsPerItem)
    }

    @Test
    fun `OBODemoComponent onSetEffectsPerItem updates state`() {
        val component = DefaultOBODemoComponent(appContext)
        component.onSetEffectsPerItem(20)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(20, component.state.value.effectsPerItem)
    }

    @Test
    fun `OBODemoComponent onSetBlockTime updates state`() {
        val component = DefaultOBODemoComponent(appContext)
        component.onSetBlockTime(5)

        assertEquals(5, component.state.value.blockTimeMs)
    }

    @Test
    fun `OBODemoComponent onReload increments trigger`() {
        val component = DefaultOBODemoComponent(appContext)
        component.onReload()

        assertEquals(1, component.state.value.reloadTrigger)
    }

    @Test
    fun `OBODemoComponent onToggleOBO updates state`() {
        val component = DefaultOBODemoComponent(appContext)
        testDispatcher.scheduler.advanceUntilIdle()

        component.onToggleOBO(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, component.state.value.useOBO)
    }

    @Test
    fun `OBODemoComponent onBackClicked pops navigator`() {
        val component = DefaultOBODemoComponent(appContext)
        component.onBackClicked()

        assertEquals(1, poppedCount.size)
    }

    @Test
    fun `OBODemoComponent updateScrollPosition updates state`() {
        val component = DefaultOBODemoComponent(appContext)
        component.updateScrollPosition(7, 150)

        assertEquals(7, component.state.value.scrollPosition.firstVisibleIndex)
        assertEquals(150, component.state.value.scrollPosition.offset)
    }

    // --- NetworkDemoComponent ---

    @Test
    fun `NetworkDemoComponent can be created and has state`() {
        val component = DefaultNetworkDemoComponent(appContext)
        assertNotNull(component.state.value)
        assertEquals(0, component.state.value.requestCount)
    }

    @Test
    fun `NetworkDemoComponent onMakeRequest triggers request`() {
        val component = DefaultNetworkDemoComponent(appContext)
        component.onMakeRequest()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, component.state.value.requestCount)
    }

    @Test
    fun `NetworkDemoComponent onNavigateToDetail pushes route`() {
        val component = DefaultNetworkDemoComponent(appContext)
        component.onNavigateToDetail("item-42")

        assertEquals(Route.Detail("item-42"), navigatedRoutes.last())
    }

    // --- ImageDemoComponent ---

    @Test
    fun `ImageDemoComponent can be created and has state`() {
        val component = DefaultImageDemoComponent(appContext)
        assertNotNull(component.state.value)
        assertTrue(component.state.value.images.isEmpty())
    }

    @Test
    fun `ImageDemoComponent loadInitial loads images`() {
        val component = DefaultImageDemoComponent(appContext)
        component.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(component.state.value.images.isNotEmpty())
    }

    @Test
    fun `ImageDemoComponent loadMore appends images`() {
        val component = DefaultImageDemoComponent(appContext)
        component.loadInitial()
        testDispatcher.scheduler.advanceUntilIdle()
        val firstCount = component.state.value.images.size

        component.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(component.state.value.images.size > firstCount)
    }

    @Test
    fun `ImageDemoComponent updateScrollPosition updates state`() {
        val component = DefaultImageDemoComponent(appContext)
        component.updateScrollPosition(3, 50)

        assertEquals(3, component.state.value.scrollPosition.firstVisibleIndex)
    }

    // --- SettingsComponent ---

    @Test
    fun `SettingsComponent can be created and has state`() {
        val component = DefaultSettingsComponent(appContext)
        assertNotNull(component.state.value)
    }

    @Test
    fun `SettingsComponent onOBOSchedulerToggle updates state`() {
        val component = DefaultSettingsComponent(appContext)
        testDispatcher.scheduler.advanceUntilIdle()

        component.onOBOSchedulerToggle(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, component.state.value.useOBOScheduler)
    }

    @Test
    fun `SettingsComponent onBackClicked pops navigator`() {
        poppedCount.clear()
        val component = DefaultSettingsComponent(appContext)
        component.onBackClicked()

        assertEquals(1, poppedCount.size)
    }
}
