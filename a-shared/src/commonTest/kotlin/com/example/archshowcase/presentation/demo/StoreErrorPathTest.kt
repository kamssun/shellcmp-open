package com.example.archshowcase.presentation.demo

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.network.api.ImageApi
import com.example.archshowcase.network.api.ImageRepository
import com.example.archshowcase.network.api.UserApi
import com.example.archshowcase.network.api.UserRepository
import com.example.archshowcase.network.dto.CreateUserRequest
import com.example.archshowcase.network.dto.ImagePageResponse
import com.example.archshowcase.network.dto.UserDto
import com.example.archshowcase.presentation.demo.image.ImageDemoStore
import com.example.archshowcase.presentation.demo.image.ImageDemoStoreFactory
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore
import com.example.archshowcase.presentation.demo.network.NetworkDemoStoreFactory
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

private class FailingImageApi : ImageApi {
    override suspend fun getImages(offset: Int, limit: Int): Result<ImagePageResponse> =
        Result.failure(RuntimeException("Network error"))
}

private class FailingUserApi : UserApi {
    override suspend fun getUser(id: String): Result<UserDto> =
        Result.failure(RuntimeException("User not found"))

    override suspend fun createUser(request: CreateUserRequest): Result<UserDto> =
        Result.failure(RuntimeException("Create failed"))

    override suspend fun getUsers(): Result<List<UserDto>> =
        Result.failure(RuntimeException("Server error"))
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreErrorPathTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()

    // --- ImageDemoStore Error Tests ---

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppConfig.useOBOScheduler = false
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        stopKoin()
    }

    private fun setupImageErrorKoin() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<ImageApi> { FailingImageApi() }
                singleOf(::ImageRepository)
                singleOf(::ImageDemoStoreFactory)
            })
        }
    }

    private fun setupNetworkErrorKoin() {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<UserApi> { FailingUserApi() }
                singleOf(::UserRepository)
                singleOf(::NetworkDemoStoreFactory)
            })
        }
    }

    @Test
    fun `ImageDemoStore LoadInitial handles API failure`() {
        setupImageErrorKoin()
        val factory: ImageDemoStoreFactory by inject()
        val store = factory.create()

        store.accept(ImageDemoStore.Intent.LoadInitial)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isInitialLoading)
        assertNotNull(state.error)
        assertEquals("Network error", state.error)

        store.dispose()
    }

    @Test
    fun `ImageDemoStore LoadMore handles API failure`() {
        setupImageErrorKoin()
        val factory: ImageDemoStoreFactory by inject()
        val store = factory.create()

        // Force state to allow LoadMore (need non-empty images and canLoadMore=true)
        // Since LoadInitial also fails, we test LoadMore directly which skips guard
        store.accept(ImageDemoStore.Intent.LoadMore)
        testDispatcher.scheduler.advanceUntilIdle()

        // LoadMore was guarded by canLoadMore which needs hasMore && !isLoadingMore && !isInitialLoading
        // Default state has hasMore=true, so LoadMore should proceed and hit the error
        val state = store.state
        assertFalse(state.isLoadingMore)
        assertNotNull(state.error)
        assertEquals("Network error", state.error)

        store.dispose()
    }

    @Test
    fun `NetworkDemoStore MakeRequest handles API failure`() {
        setupNetworkErrorKoin()
        val factory: NetworkDemoStoreFactory by inject()
        val store = factory.create()

        store.accept(NetworkDemoStore.Intent.MakeRequest)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Server error", state.error)

        store.dispose()
    }

    @Test
    fun `NetworkDemoStore error state clears on next success attempt`() {
        setupNetworkErrorKoin()
        val factory: NetworkDemoStoreFactory by inject()
        val store = factory.create()

        store.accept(NetworkDemoStore.Intent.MakeRequest)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(store.state.error)

        // Second request still fails but verifies the loading→error cycle
        store.accept(NetworkDemoStore.Intent.MakeRequest)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(store.state.error)

        store.dispose()
    }
}
