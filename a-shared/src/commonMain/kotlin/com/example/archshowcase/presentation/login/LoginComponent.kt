package com.example.archshowcase.presentation.login

import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.isIosPlatform
import com.example.archshowcase.auth.LoginType
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LoginComponent {
    val state: StateFlow<LoginStore.State>
    val showAppleLogin: Boolean

    fun onGoogleLogin()
    fun onAppleLogin()
    fun onEmailLogin()
    fun onDismissError()
}

class DefaultLoginComponent(
    context: AppComponentContext
) : LoginComponent, AppComponentContext by context, KoinComponent {

    init {
        loadLoginModule()
    }

    private val storeFactory: LoginStoreFactory by inject()
    private val store = instanceKeeper.getStore { storeFactory.create() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<LoginStore.State> = store.stateFlow

    override val showAppleLogin: Boolean = isIosPlatform

    override fun onGoogleLogin() {
        store.accept(LoginStore.Intent.Login(LoginType.GOOGLE))
    }

    override fun onAppleLogin() {
        store.accept(LoginStore.Intent.Login(LoginType.APPLE))
    }

    override fun onEmailLogin() {
        navigator.push(Route.EmailLogin)
    }

    override fun onDismissError() {
        store.accept(LoginStore.Intent.ClearError)
    }
}
