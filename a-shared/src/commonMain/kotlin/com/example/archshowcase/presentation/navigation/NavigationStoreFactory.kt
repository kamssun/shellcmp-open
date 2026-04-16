package com.example.archshowcase.presentation.navigation

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.presentation.navigation.NavigationStore.Intent
import com.example.archshowcase.presentation.navigation.NavigationStore.Msg
import com.example.archshowcase.presentation.navigation.NavigationStore.State
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NavigationStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()

    fun create(name: String = NAVIGATION_STORE_NAME): NavigationStore {
        Log.d(TAG) { "Creating NavigationStore '$name' with factory: ${storeFactory::class.simpleName}" }
        val initialState = RestoreRegistry.resolveInitialState<State>(name) ?: State()
        return object : NavigationStore,
            Store<Intent, State, Nothing> by storeFactory.create(
                name = name,
                initialState = initialState,
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl
            ) {}
    }

    @OptIn(ExperimentalTime::class)
    private class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Nothing>() {

        override fun executeIntent(intent: Intent) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            when (intent) {
                is Intent.Push -> {
                    val record = NavHistoryRecord(NavHistoryType.Push(intent.route.serialName), timestamp)
                    Log.d(TAG) { "Push: ${intent.route.serialName}" }
                    dispatch(Msg.Pushed(intent.route, record))
                }
                is Intent.Pop -> {
                    val currentState = state()
                    val record = NavHistoryRecord(NavHistoryType.Pop(currentState.currentRoute.serialName), timestamp)
                    Log.d(TAG) { "Pop from: ${currentState.currentRoute.serialName}" }
                    dispatch(Msg.Popped(record))
                }
                is Intent.BringToFront -> {
                    val record = NavHistoryRecord(NavHistoryType.BringToFront(intent.route.serialName), timestamp)
                    Log.d(TAG) { "BringToFront: ${intent.route.serialName}" }
                    dispatch(Msg.BroughtToFront(intent.route, record))
                }
                is Intent.ReplaceAll -> {
                    val routeNames = intent.routes.joinToString(",") { it.serialName }
                    val record = NavHistoryRecord(NavHistoryType.ReplaceAll(routeNames), timestamp)
                    Log.d(TAG) { "ReplaceAll: $routeNames" }
                    dispatch(Msg.ReplacedAll(intent.routes, record))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State {
            val record = when (msg) {
                is Msg.Pushed -> msg.record
                is Msg.Popped -> msg.record
                is Msg.BroughtToFront -> msg.record
                is Msg.ReplacedAll -> msg.record
            }

            return record.applyToState(this)
        }
    }

    companion object {
        private const val TAG = "Navigation"
    }
}
