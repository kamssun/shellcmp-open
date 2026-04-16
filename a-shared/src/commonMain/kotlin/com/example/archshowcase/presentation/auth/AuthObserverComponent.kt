package com.example.archshowcase.presentation.auth

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.scheduler.oboLaunch
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.auth.AuthService
import com.example.archshowcase.auth.AuthState
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.Route
import com.example.archshowcase.user.UserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface AuthObserverComponent

class DefaultAuthObserverComponent(
    context: AppComponentContext
) : AuthObserverComponent, AppComponentContext by context, KoinComponent {

    companion object {
        private const val TAG = "Auth"
        private const val OBO_TAG = "AuthObserver"
    }

    private val authService: AuthService by inject()
    private val userService: UserService by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.oboLaunch(OBO_TAG) {
            authService.authState.collect { authState ->
                val current = navigator.currentRoute
                when (authState) {
                    is AuthState.LoggedIn -> {
                        if (current is Route.Login || current is Route.EmailLogin) {
                            scope.oboLaunch(OBO_TAG) {
                                userService.fetchProfile()
                                    .onFailure { Log.w(TAG) { "fetchProfile failed: ${it.message}" } }
                            }
                            navigator.replaceAll(Route.Home)
                        }
                    }
                    is AuthState.LoggedOut -> {
                        userService.clear()
                        if (!AppConfig.skipLogin && current !is Route.Login && current !is Route.EmailLogin) {
                            navigator.replaceAll(Route.Login)
                        }
                    }
                    is AuthState.ForcedLogout -> {
                        Log.w(TAG) { "Forced logout: ${authState.reason}" }
                        userService.clear()
                        if (!AppConfig.skipLogin && current !is Route.Login) {
                            navigator.replaceAll(Route.Login)
                        }
                    }
                }
            }
        }

        lifecycle.doOnDestroy { scope.cancel() }
    }
}
