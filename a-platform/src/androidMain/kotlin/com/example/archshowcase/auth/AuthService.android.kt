// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.auth

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.core.AppRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class AuthService actual constructor() {

    actual val authState: StateFlow<AuthState> = sharedAuthState.asStateFlow()

    init {
        if (!AppRuntimeState.isInPreview) {
            refreshStateFromSdk()
        }
    }

    actual fun isLoggedIn(): Boolean = getAccessToken() != null

    actual fun getAccessToken(): String? {
        // Stub: SDK removed — always returns null
        return null
    }

    actual suspend fun login(type: LoginType, platformContext: Any?): Result<AuthToken> {
        // Stub: SDK removed — login not available
        return Result.failure(NotImplementedError("Auth SDK stub"))
    }

    actual suspend fun sendEmailCode(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_EMAIL))
        }
        // Stub: SDK removed — pretend code was sent
        return Result.success(Unit)
    }

    actual suspend fun verifyEmailCode(email: String, code: String): Result<AuthToken> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_EMAIL))
        }
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_CODE))
        }
        // Stub: SDK removed — verification not available
        return Result.failure(NotImplementedError("Auth SDK stub"))
    }

    actual suspend fun refreshToken(): Result<AuthToken> {
        // Stub: SDK removed — token refresh not available
        return Result.failure(NotImplementedError("Auth SDK stub"))
    }

    actual suspend fun logout() {
        lastRefreshToken = ""
        sharedAuthState.value = AuthState.LoggedOut
    }

    companion object {
        private const val TAG = "AuthService"

        private val sharedAuthState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
        private var lastRefreshToken: String = ""

        internal fun onForcedLogout(code: Int, message: String) {
            lastRefreshToken = ""
            sharedAuthState.value = AuthState.ForcedLogout("$code:$message")
        }

        internal fun refreshStateFromSdk() {
            // Stub: SDK removed — no token to read
            if (sharedAuthState.value !is AuthState.ForcedLogout) {
                sharedAuthState.value = AuthState.LoggedOut
            }
        }
    }
}
