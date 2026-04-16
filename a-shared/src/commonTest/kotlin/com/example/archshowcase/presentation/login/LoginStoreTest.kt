package com.example.archshowcase.presentation.login

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.auth.AuthToken
import com.example.archshowcase.auth.LoginRepository
import com.example.archshowcase.auth.LoginType
import com.example.archshowcase.presentation.login.LoginStore.Intent
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeLoginRepository : LoginRepository {
    override suspend fun login(type: LoginType) = Result.success(
        AuthToken(
            accessToken = "test_access_${type.name}",
            refreshToken = "test_refresh_${type.name}"
        )
    )

    override suspend fun sendEmailCode(email: String) = Result.success(Unit)

    override suspend fun verifyEmailCode(email: String, code: String) = Result.success(
        AuthToken(accessToken = "test_email_access", refreshToken = "test_email_refresh")
    )
}

private class FailingLoginRepository : LoginRepository {
    override suspend fun login(type: LoginType): Result<AuthToken> =
        Result.failure(RuntimeException("认证服务不可用"))

    override suspend fun sendEmailCode(email: String): Result<Unit> =
        Result.failure(RuntimeException("发送失败"))

    override suspend fun verifyEmailCode(email: String, code: String): Result<AuthToken> =
        Result.failure(RuntimeException("验证码错误"))
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginStoreTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: LoginStore

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppConfig.useOBOScheduler = false
    }

    @AfterTest
    fun teardown() {
        if (::store.isInitialized) store.dispose()
        Dispatchers.resetMain()
        AppConfig.useOBOScheduler = true
        try { stopKoin() } catch (_: IllegalStateException) { }
    }

    // Koin setup deferred to per-test helpers (success vs failure repository)
    private fun setupKoin(repository: LoginRepository) {
        startKoin {
            modules(module {
                single<StoreFactory> { DefaultStoreFactory() }
                single<LoginRepository> { repository }
                singleOf(::LoginStoreFactory)
            })
        }
        val factory: LoginStoreFactory by inject()
        store = factory.create()
    }

    private fun setupWithSuccessRepo() = setupKoin(FakeLoginRepository())
    private fun setupWithFailingRepo() = setupKoin(FailingLoginRepository())

    // --- Initial State ---

    @Test
    fun `initial state is idle`() {
        setupWithSuccessRepo()
        val state = store.state
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.emailCodeSent)
        assertEquals("", state.currentEmail)
    }

    // --- Login Intent ---

    @Test
    fun `Login success clears loading and error`() {
        setupWithSuccessRepo()
        store.accept(Intent.Login(LoginType.GOOGLE))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `Login failure sets error message`() {
        setupWithFailingRepo()
        store.accept(Intent.Login(LoginType.GOOGLE))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertEquals("认证服务不可用", state.errorMessage)
    }

    // --- SendEmailCode Intent ---

    @Test
    fun `SendEmailCode success updates emailCodeSent and currentEmail`() {
        setupWithSuccessRepo()
        store.accept(Intent.SendEmailCode("user@example.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.emailCodeSent)
        assertEquals("user@example.com", state.currentEmail)
    }

    @Test
    fun `SendEmailCode failure sets error message`() {
        setupWithFailingRepo()
        store.accept(Intent.SendEmailCode("user@example.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertEquals("发送失败", state.errorMessage)
        assertFalse(state.emailCodeSent)
    }

    // --- VerifyEmailCode Intent ---

    @Test
    fun `VerifyEmailCode success clears loading`() {
        setupWithSuccessRepo()
        store.accept(Intent.VerifyEmailCode("user@example.com", "123456"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `VerifyEmailCode failure sets error message`() {
        setupWithFailingRepo()
        store.accept(Intent.VerifyEmailCode("user@example.com", "123456"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertFalse(state.isLoading)
        assertEquals("验证码错误", state.errorMessage)
    }

    // --- ClearError Intent ---

    @Test
    fun `ClearError clears error message`() {
        setupWithFailingRepo()
        store.accept(Intent.Login(LoginType.GOOGLE))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(store.state.errorMessage)

        store.accept(Intent.ClearError)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(store.state.errorMessage)
    }
}
