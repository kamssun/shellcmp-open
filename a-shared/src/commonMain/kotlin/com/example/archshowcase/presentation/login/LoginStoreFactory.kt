package com.example.archshowcase.presentation.login

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.scheduler.oboLaunch
import com.example.archshowcase.auth.LoginRepository
import com.example.archshowcase.presentation.login.LoginStore.Intent
import com.example.archshowcase.presentation.login.LoginStore.Label
import com.example.archshowcase.presentation.login.LoginStore.Msg
import com.example.archshowcase.presentation.login.LoginStore.State
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val loginRepository: LoginRepository by inject()

    fun create(): LoginStore =
        object : LoginStore,
            Store<Intent, State, Label> by storeFactory.create(
                name = STORE_NAME,
                initialState = State(),
                executorFactory = { ExecutorImpl(loginRepository) },
                reducer = ReducerImpl
            ) {}

    private class ExecutorImpl(
        private val loginRepository: LoginRepository
    ) : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.Login -> login(intent)
                is Intent.SendEmailCode -> sendEmailCode(intent)
                is Intent.VerifyEmailCode -> verifyEmailCode(intent)
                is Intent.ClearError -> dispatch(Msg.ClearError)
            }
        }

        private fun login(intent: Intent.Login) {
            scope.oboLaunch(OBO_TAG) {
                dispatch(Msg.LoadingStarted)
                loginRepository.login(intent.type)
                    .onSuccess { token ->
                        dispatch(Msg.LoginSucceeded(token))
                        publish(Label.LoginCompleted(token))
                    }
                    .onFailure { error ->
                        dispatch(Msg.Error(error.message ?: "登录失败"))
                    }
            }
        }

        private fun sendEmailCode(intent: Intent.SendEmailCode) {
            scope.oboLaunch(OBO_TAG) {
                dispatch(Msg.LoadingStarted)
                loginRepository.sendEmailCode(intent.email)
                    .onSuccess {
                        dispatch(Msg.EmailCodeSent(intent.email))
                        publish(Label.EmailCodeSent(intent.email))
                    }
                    .onFailure { error ->
                        dispatch(Msg.Error(error.message ?: "发送验证码失败"))
                    }
            }
        }

        private fun verifyEmailCode(intent: Intent.VerifyEmailCode) {
            scope.oboLaunch(OBO_TAG) {
                dispatch(Msg.LoadingStarted)
                loginRepository.verifyEmailCode(intent.email, intent.code)
                    .onSuccess { token ->
                        dispatch(Msg.LoginSucceeded(token))
                        publish(Label.LoginCompleted(token))
                    }
                    .onFailure { error ->
                        dispatch(Msg.Error(error.message ?: "验证码登录失败"))
                    }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.LoadingStarted -> copy(isLoading = true, errorMessage = null)
            is Msg.LoginSucceeded -> copy(isLoading = false, errorMessage = null)
            is Msg.EmailCodeSent -> copy(
                isLoading = false,
                errorMessage = null,
                emailCodeSent = true,
                currentEmail = msg.email
            )
            is Msg.Error -> copy(isLoading = false, errorMessage = msg.message)
            is Msg.ClearError -> copy(errorMessage = null)
        }
    }

    companion object {
        private const val STORE_NAME = "LoginStore"
        private const val OBO_TAG = "LoginStore"
    }
}
