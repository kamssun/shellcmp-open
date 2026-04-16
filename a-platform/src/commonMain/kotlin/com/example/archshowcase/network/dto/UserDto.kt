package com.example.archshowcase.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)
