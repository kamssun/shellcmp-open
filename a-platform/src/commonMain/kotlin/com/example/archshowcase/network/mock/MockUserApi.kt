package com.example.archshowcase.network.mock

import com.example.archshowcase.network.api.UserApi
import com.example.archshowcase.network.dto.CreateUserRequest
import com.example.archshowcase.network.dto.UserDto
import com.example.archshowcase.core.util.Log

class MockUserApi : UserApi {
    companion object {
        private const val TAG = "MockUserApi"

        private val mockUsers = listOf(
            UserDto(id = 1, name = "Mock User 1", email = "user1@mock.com"),
            UserDto(id = 2, name = "Mock User 2", email = "user2@mock.com"),
            UserDto(id = 3, name = "Mock User 3", email = "user3@mock.com")
        )
    }

    override suspend fun getUser(id: String): Result<UserDto> = runCatching {
        Log.d(TAG) { "Mock fetching user: $id" }
        mockUsers.firstOrNull { it.id.toString() == id }
            ?: throw NoSuchElementException("User not found: $id")
    }

    override suspend fun createUser(request: CreateUserRequest): Result<UserDto> = runCatching {
        Log.d(TAG) { "Mock creating user: ${request.name}" }
        UserDto(id = nextId++, name = request.name, email = request.email)
    }

    private var nextId = 100

    override suspend fun getUsers(): Result<List<UserDto>> = runCatching {
        Log.d(TAG) { "Mock fetching all users" }
        mockUsers
    }
}
