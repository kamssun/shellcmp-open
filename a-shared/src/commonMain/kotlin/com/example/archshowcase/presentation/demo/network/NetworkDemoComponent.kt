package com.example.archshowcase.presentation.demo.network

import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.trace.restore.registerRestorableStore
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface NetworkDemoComponent {
    val state: StateFlow<NetworkDemoStore.State>

    fun onBack()
    fun onMakeRequest()
    fun onNavigateToDetail(id: String)
}

class DefaultNetworkDemoComponent(
    context: AppComponentContext
) : NetworkDemoComponent, AppComponentContext by context, KoinComponent {

    init { loadNetworkDemoModule() }

    private val storeFactory: NetworkDemoStoreFactory by inject()

    private val store = registerRestorableStore(
        name = NETWORK_DEMO_STORE_NAME,
        factory = { storeFactory.create() }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<NetworkDemoStore.State> = store.stateFlow

    override fun onMakeRequest() {
        store.accept(NetworkDemoStore.Intent.MakeRequest)
    }

    override fun onNavigateToDetail(id: String) {
        navigator.push(Route.Detail(id))
    }
}
