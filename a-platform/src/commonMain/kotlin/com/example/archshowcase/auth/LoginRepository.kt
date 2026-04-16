package com.example.archshowcase.auth

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** AuthService 是 expect class，无法 fake；通过此接口解耦 Store 层 */
interface LoginRepository {
    suspend fun login(type: LoginType): Result<AuthToken>
    suspend fun sendEmailCode(email: String): Result<Unit>
    suspend fun verifyEmailCode(email: String, code: String): Result<AuthToken>
}

class DefaultLoginRepository : KoinComponent, LoginRepository {
    private val authService: AuthService by inject()

    override suspend fun login(type: LoginType) = authService.login(type)
    override suspend fun sendEmailCode(email: String) = authService.sendEmailCode(email)
    override suspend fun verifyEmailCode(email: String, code: String) =
        authService.verifyEmailCode(email, code)
}
