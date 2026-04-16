package com.example.archshowcase.presentation.settings

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.data.settings.SettingsRepository
import com.example.archshowcase.presentation.settings.SettingsStore.Action
import com.example.archshowcase.presentation.settings.SettingsStore.Intent
import com.example.archshowcase.presentation.settings.SettingsStore.Msg
import com.example.archshowcase.presentation.settings.SettingsStore.State
import kotlinx.coroutines.flow.first
import com.example.archshowcase.core.scheduler.oboLaunch
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val repository: SettingsRepository by inject()
    companion object {
        private const val TAG = "SettingsStore"
        private const val OBO_TAG = "SettingsStore"
    }

    fun create(): SettingsStore {
        // 使用 KSP 生成的辅助函数
        val (initialState, needsBootstrap) = resolveSettingsStoreInitialState()

        return object : SettingsStore,
            Store<Intent, State, Nothing> by storeFactory.create(
                name = SETTINGS_STORE_NAME,
                initialState = initialState,
                bootstrapper = if (needsBootstrap) BootstrapperImpl(repository) else null,
                executorFactory = { ExecutorImpl(repository) },
                reducer = ReducerImpl
            ) {}
    }

    private class BootstrapperImpl(
        private val repository: SettingsRepository
    ) : CoroutineBootstrapper<Action>() {
        override fun invoke() {
            scope.oboLaunch(OBO_TAG) {
                val useOBO = repository.useOBOScheduler.first()
                dispatch(Action.LoadedSettings(useOBO))
            }
        }
    }

    private class ExecutorImpl(
        private val repository: SettingsRepository
    ) : CoroutineExecutor<Intent, Action, State, Msg, Nothing>() {

        override fun executeAction(action: Action) {
            when (action) {
                is Action.LoadedSettings -> dispatch(Msg.SettingsLoaded(action.useOBOScheduler))
            }
        }

        override fun executeIntent(intent: Intent) {
            when (intent) {
                is Intent.SetOBOScheduler -> setOBOScheduler(intent.enabled)
            }
        }

        private fun setOBOScheduler(enabled: Boolean) {
            val oldValue = state().useOBOScheduler
            val timestamp = Clock.System.now().toEpochMilliseconds()
            scope.oboLaunch(OBO_TAG) {
                repository.setUseOBOScheduler(enabled)
                dispatch(Msg.OBOSchedulerUpdated(oldValue, enabled, timestamp))
            }
        }
    }

    private object ReducerImpl : com.arkivanov.mvikotlin.core.store.Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State = when (msg) {
            is Msg.SettingsLoaded -> copy(useOBOScheduler = msg.useOBOScheduler)
            is Msg.OBOSchedulerUpdated -> {
                val record = SettingsHistoryRecord(
                    type = SettingsHistoryType.SetOBOScheduler(msg.newValue),
                    timestamp = msg.timestamp
                )
                record.applyToState(this)
            }
        }
    }
}
