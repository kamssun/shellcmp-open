package com.example.archshowcase.presentation.demo.network

import com.example.archshowcase.core.util.Log
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.network.api.UserRepository
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.Intent
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.Label
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.Msg
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.State
import com.example.archshowcase.core.scheduler.oboLaunch
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NetworkDemoStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val repository: UserRepository by inject()
    companion object {
        private const val TAG = "NetworkDemo"
        private const val OBO_TAG = "NetworkDemoStore"
    }

    fun create(): NetworkDemoStore {
        val (initialState, _) = resolveNetworkDemoStoreInitialState()

        return object : NetworkDemoStore,
            Store<Intent, State, Label> by storeFactory.create(
                name = NETWORK_DEMO_STORE_NAME,
                initialState = initialState,
                executorFactory = { ExecutorImpl(repository) },
                reducer = ReducerImpl
            ) {}
    }

    private class ExecutorImpl(
        private val repository: UserRepository
    ) : CoroutineExecutor<Intent, Nothing, State, Msg, Label>() {

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.MakeRequest -> makeRequest()
            }
        }

        private fun makeRequest() {
            scope.oboLaunch(OBO_TAG) {
                dispatch(Msg.LoadingStarted)

                val count = state().requestCount + 1
                Log.d(TAG) { "Starting request #$count" }

                val timestamp = Clock.System.now().toEpochMilliseconds()
                repository.getUsers()
                    .onSuccess { users ->
                        val names = users.take(5).joinToString(", ") { it.name }
                        val summary = "获取 ${users.size} 个用户: $names"
                        dispatch(Msg.RequestSuccess(count, summary, timestamp))
                        publish(Label.RequestCompleted(count))
                    }
                    .onFailure { error ->
                        dispatch(Msg.RequestFailed(error.message ?: "请求失败"))
                        publish(Label.RequestFailed(error.message ?: "请求失败"))
                    }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.LoadingStarted -> copy(isLoading = true, error = null)

            is Msg.RequestSuccess -> {
                val record = NetworkHistoryRecord(
                    type = NetworkHistoryType.Request(count = msg.count, result = msg.result),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }

            is Msg.RequestFailed -> copy(isLoading = false, error = msg.error)
        }
    }
}
