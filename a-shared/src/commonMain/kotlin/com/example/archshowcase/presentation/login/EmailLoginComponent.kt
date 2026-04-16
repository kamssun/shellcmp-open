package com.example.archshowcase.presentation.login

import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.presentation.navigation.AppComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface EmailLoginComponent {
    val state: StateFlow<LoginStore.State>

    fun onSendCode(email: String)
    fun onVerifyCode(email: String, code: String)
    fun onDismissError()
    fun onBack()
}

class DefaultEmailLoginComponent(
    context: AppComponentContext
) : EmailLoginComponent, AppComponentContext by context, KoinComponent {

    init {
        loadLoginModule()
    }

    private val storeFactory: LoginStoreFactory by inject()
    private val store = instanceKeeper.getStore { storeFactory.create() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<LoginStore.State> = store.stateFlow

    override fun onSendCode(email: String) {
        store.accept(LoginStore.Intent.SendEmailCode(email))
    }

    override fun onVerifyCode(email: String, code: String) {
        store.accept(LoginStore.Intent.VerifyEmailCode(email, code))
    }

    override fun onDismissError() {
        store.accept(LoginStore.Intent.ClearError)
    }

}
