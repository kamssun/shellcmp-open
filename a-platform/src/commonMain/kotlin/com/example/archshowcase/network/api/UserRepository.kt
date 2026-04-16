package com.example.archshowcase.network.api

import com.example.archshowcase.network.dto.CreateUserRequest
import com.example.archshowcase.network.dto.UserDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserRepository : KoinComponent {

    private val api: UserApi by inject()

    suspend fun getUser(id: String): Result<UserDto> =
        api.getUser(id)

    suspend fun createUser(name: String, email: String): Result<UserDto> =
        api.createUser(CreateUserRequest(name, email))

    suspend fun getUsers(): Result<List<UserDto>> =
        api.getUsers()
}
