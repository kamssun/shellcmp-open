package com.example.archshowcase.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class AuthService actual constructor() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    actual val authState: StateFlow<AuthState> = _authState.asStateFlow()

    actual fun isLoggedIn(): Boolean = _authState.value is AuthState.LoggedIn

    actual fun getAccessToken(): String? {
        val state = _authState.value as? AuthState.LoggedIn
        return state?.token?.accessToken
    }

    actual suspend fun login(type: LoginType, platformContext: Any?): Result<AuthToken> {
        delay(500)
        val token = AuthToken(
            accessToken = "desktop_access_${type.name.lowercase()}",
            refreshToken = "desktop_refresh_${type.name.lowercase()}"
        )
        _authState.value = AuthState.LoggedIn(token)
        return Result.success(token)
    }

    actual suspend fun sendEmailCode(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_EMAIL))
        }
        delay(300)
        return Result.success(Unit)
    }

    actual suspend fun verifyEmailCode(email: String, code: String): Result<AuthToken> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_EMAIL))
        }
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_CODE))
        }

        delay(500)
        val token = AuthToken(
            accessToken = "desktop_access_email",
            refreshToken = "desktop_refresh_email"
        )
        _authState.value = AuthState.LoggedIn(token)
        return Result.success(token)
    }

    actual suspend fun refreshToken(): Result<AuthToken> {
        val current = _authState.value as? AuthState.LoggedIn
            ?: return Result.failure(IllegalStateException(AuthConstants.ERROR_REFRESH_FORBIDDEN))
        delay(300)
        val refreshed = AuthToken(
            accessToken = "desktop_refreshed_${System.currentTimeMillis()}",
            refreshToken = current.token.refreshToken
        )
        _authState.value = AuthState.LoggedIn(refreshed)
        return Result.success(refreshed)
    }

    actual suspend fun logout() {
        _authState.value = AuthState.LoggedOut
    }
}
