package com.example.archshowcase.auth

import kotlinx.coroutines.flow.StateFlow

expect class AuthService() {
    val authState: StateFlow<AuthState>

    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?

    suspend fun login(type: LoginType, platformContext: Any? = null): Result<AuthToken>
    suspend fun sendEmailCode(email: String): Result<Unit>
    suspend fun verifyEmailCode(email: String, code: String): Result<AuthToken>
    suspend fun refreshToken(): Result<AuthToken>
    suspend fun logout()
}
