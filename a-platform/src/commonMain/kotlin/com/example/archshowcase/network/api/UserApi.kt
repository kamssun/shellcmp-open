package com.example.archshowcase.network.api

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.network.ApiRoutes
import com.example.archshowcase.network.dto.CreateUserRequest
import com.example.archshowcase.network.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 用户 API 接口
 */
interface UserApi {
    suspend fun getUser(id: String): Result<UserDto>
    suspend fun createUser(request: CreateUserRequest): Result<UserDto>
    suspend fun getUsers(): Result<List<UserDto>>
}

/**
 * 真实实现 - 使用 Ktor 发起网络请求
 */
class DefaultUserApi : UserApi, KoinComponent {

    private val client: HttpClient by inject()

    companion object {
        private const val TAG = "UserApi"
    }

    override suspend fun getUser(id: String): Result<UserDto> = runCatching {
        Log.d(TAG) { "Fetching user: $id" }
        client.get("${ApiRoutes.USERS}/$id").body<UserDto>()
    }.onFailure { e ->
        Log.e(TAG, e) { "Failed to fetch user: $id" }
    }

    override suspend fun createUser(request: CreateUserRequest): Result<UserDto> = runCatching {
        Log.d(TAG) { "Creating user: ${request.name}" }
        client.post(ApiRoutes.USERS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<UserDto>()
    }.onFailure { e ->
        Log.e(TAG, e) { "Failed to create user" }
    }

    override suspend fun getUsers(): Result<List<UserDto>> = runCatching {
        Log.d(TAG) { "Fetching all users" }
        client.get(ApiRoutes.USERS).body<List<UserDto>>()
    }.onFailure { e ->
        Log.e(TAG, e) { "Failed to fetch users" }
    }
}
