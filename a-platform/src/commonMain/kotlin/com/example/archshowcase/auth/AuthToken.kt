package com.example.archshowcase.auth

import com.arkivanov.mvikotlin.core.utils.JvmSerializable

/**
 * 登录后的业务 Token
 */
data class AuthToken(
    val accessToken: String,
    val refreshToken: String
) : JvmSerializable
