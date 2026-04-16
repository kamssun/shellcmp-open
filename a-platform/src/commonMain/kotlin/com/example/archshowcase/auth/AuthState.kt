package com.example.archshowcase.auth

import com.arkivanov.mvikotlin.core.utils.JvmSerializable

sealed interface AuthState : JvmSerializable {
    data object LoggedOut : AuthState
    data class LoggedIn(val token: AuthToken) : AuthState
    data class ForcedLogout(val reason: String) : AuthState
}
