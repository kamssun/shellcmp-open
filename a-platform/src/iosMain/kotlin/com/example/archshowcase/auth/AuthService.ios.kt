package com.example.archshowcase.auth

import com.example.archshowcase.getLoginBridgeOrNull
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

actual class AuthService actual constructor() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    actual val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        refreshAuthState()
        getLoginBridgeOrNull()?.setListener(object : LoginBridgeListener {
            override fun onForcedLogout(reason: String) {
                _authState.value = AuthState.ForcedLogout(reason)
            }
        })
    }

    actual fun isLoggedIn(): Boolean = getLoginBridgeOrNull()?.isLoggedIn() == true

    actual fun getAccessToken(): String? {
        return getLoginBridgeOrNull()
            ?.getAccessToken()
            ?.takeIf { it.isNotBlank() }
    }

    actual suspend fun login(type: LoginType, platformContext: Any?): Result<AuthToken> {
        val bridge = getLoginBridgeOrNull()
            ?: return Result.failure(IllegalStateException(AuthConstants.ERROR_BRIDGE_NOT_SET))

        return suspendCancellableCoroutine { continuation ->
            bridge.login(type, object : LoginTokenCallback {
                override fun onSuccess(accessToken: String, refreshToken: String) {
                    val token = AuthToken(accessToken = accessToken, refreshToken = refreshToken)
                    _authState.value = AuthState.LoggedIn(token)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(token))
                    }
                }

                override fun onFailure(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            })
        }
    }

    actual suspend fun sendEmailCode(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_EMAIL))
        }

        val bridge = getLoginBridgeOrNull()
            ?: return Result.failure(IllegalStateException(AuthConstants.ERROR_BRIDGE_NOT_SET))

        return suspendCancellableCoroutine { continuation ->
            bridge.sendEmailCode(email, object : LoginUnitCallback {
                override fun onSuccess() {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }

                override fun onFailure(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            })
        }
    }

    actual suspend fun verifyEmailCode(email: String, code: String): Result<AuthToken> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_EMAIL))
        }
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException(AuthConstants.ERROR_EMPTY_CODE))
        }

        val bridge = getLoginBridgeOrNull()
            ?: return Result.failure(IllegalStateException(AuthConstants.ERROR_BRIDGE_NOT_SET))

        return suspendCancellableCoroutine { continuation ->
            bridge.verifyEmailCode(email, code, object : LoginTokenCallback {
                override fun onSuccess(accessToken: String, refreshToken: String) {
                    val token = AuthToken(accessToken = accessToken, refreshToken = refreshToken)
                    _authState.value = AuthState.LoggedIn(token)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(token))
                    }
                }

                override fun onFailure(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            })
        }
    }

    actual suspend fun refreshToken(): Result<AuthToken> {
        val bridge = getLoginBridgeOrNull()
            ?: return Result.failure(IllegalStateException(AuthConstants.ERROR_BRIDGE_NOT_SET))

        return suspendCancellableCoroutine { continuation ->
            bridge.refreshToken(object : LoginTokenCallback {
                override fun onSuccess(accessToken: String, refreshToken: String) {
                    val token = AuthToken(accessToken = accessToken, refreshToken = refreshToken)
                    _authState.value = AuthState.LoggedIn(token)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(token))
                    }
                }

                override fun onFailure(message: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(IllegalStateException(message)))
                    }
                }
            })
        }
    }

    actual suspend fun logout() {
        val bridge = getLoginBridgeOrNull()
            ?: return

        suspendCancellableCoroutine<Unit> { continuation ->
            bridge.logout(object : LoginUnitCallback {
                override fun onSuccess() {
                    _authState.value = AuthState.LoggedOut
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onFailure(message: String) {
                    _authState.value = AuthState.LoggedOut
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            })
        }
    }

    private fun refreshAuthState() {
        val accessToken = getAccessToken()
        if (accessToken.isNullOrBlank()) {
            _authState.value = AuthState.LoggedOut
            return
        }

        _authState.value = AuthState.LoggedIn(
            AuthToken(
                accessToken = accessToken,
                refreshToken = ""
            )
        )
    }
}
