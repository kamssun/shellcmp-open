package com.example.archshowcase.auth

import com.arkivanov.mvikotlin.core.utils.JvmSerializable

enum class LoginType : JvmSerializable {
    GOOGLE,
    APPLE,
    EMAIL
}
