package com.example.archshowcase.presentation.main

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.presentation.main.MainTabStore.Intent
import com.example.archshowcase.presentation.main.MainTabStore.Msg
import com.example.archshowcase.presentation.main.MainTabStore.State
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainTabStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()

    fun create(): MainTabStore {
        val (initialState, needsBootstrap) = resolveMainTabStoreInitialState()
        return object : MainTabStore,
            Store<Intent, State, Nothing> by storeFactory.create(
                name = MAIN_TAB_STORE_NAME,
                initialState = initialState,
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl
            ) {}
    }

    private class ExecutorImpl : CoroutineExecutor<Intent, Nothing, State, Msg, Nothing>() {
        override fun executeIntent(intent: Intent) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            when (intent) {
                is Intent.SelectTab -> {
                    val record = MainTabHistoryRecord(
                        MainTabHistoryType.SelectTab(intent.index),
                        timestamp
                    )
                    dispatch(Msg.TabSelected(intent.index, record))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.TabSelected -> msg.record.applyToState(this)
        }
    }
}
