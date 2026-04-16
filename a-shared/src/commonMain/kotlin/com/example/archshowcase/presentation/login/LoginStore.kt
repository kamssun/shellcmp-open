package com.example.archshowcase.presentation.login

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.auth.AuthToken
import com.example.archshowcase.auth.LoginType
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.login.LoginStore.Intent
import com.example.archshowcase.presentation.login.LoginStore.Label
import com.example.archshowcase.presentation.login.LoginStore.State

@VfResolvable
interface LoginStore : Store<Intent, State, Label> {

    sealed interface Intent : JvmSerializable {
        data class Login(val type: LoginType) : Intent
        data class SendEmailCode(val email: String) : Intent
        data class VerifyEmailCode(val email: String, val code: String) : Intent
        data object ClearError : Intent
    }

    @CustomState
    data class State(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val emailCodeSent: Boolean = false,
        val currentEmail: String = ""
    ) : JvmSerializable

    sealed interface Label : JvmSerializable {
        data class LoginCompleted(val token: AuthToken) : Label
        data class EmailCodeSent(val email: String) : Label
    }

    sealed interface Msg : JvmSerializable {
        data object LoadingStarted : Msg
        data class LoginSucceeded(val token: AuthToken) : Msg
        data class EmailCodeSent(val email: String) : Msg
        data class Error(val message: String) : Msg
        data object ClearError : Msg
    }
}
