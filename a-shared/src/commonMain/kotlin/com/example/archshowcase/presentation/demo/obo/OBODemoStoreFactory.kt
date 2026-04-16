package com.example.archshowcase.presentation.demo.obo

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.data.settings.SettingsRepository
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.Action
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.Intent
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.Msg
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.State
import kotlinx.coroutines.flow.first
import com.example.archshowcase.core.scheduler.oboLaunch
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OBODemoStoreFactory : KoinComponent {
    private val storeFactory: StoreFactory by inject()
    private val repository: SettingsRepository by inject()

    companion object {
        private const val OBO_TAG = "OBODemoStore"
    }
    fun create(): OBODemoStore {
        val (initialState, needsBootstrap) = resolveOBODemoStoreInitialState()

        return object : OBODemoStore,
            Store<Intent, State, Nothing> by storeFactory.create(
                name = OBODEMO_STORE_NAME,
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
                is Action.LoadedSettings -> dispatch(Msg.SettingsLoaded(action.useOBO))
            }
        }

        override fun executeIntent(intent: Intent) {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            when (intent) {
                is Intent.SetEffectsPerItem -> dispatch(Msg.EffectsPerItemChanged(intent.count, timestamp))
                is Intent.SetBlockTime -> dispatch(Msg.BlockTimeChanged(intent.ms, timestamp))
                is Intent.Reload -> dispatch(Msg.Reloaded(timestamp))
                is Intent.ToggleOBO -> toggleOBO(intent.enabled, timestamp)
                is Intent.UpdateScrollPosition -> {
                    val position = ScrollPosition(intent.firstVisibleIndex, intent.offset)
                    dispatch(Msg.ScrollPositionUpdated(position, timestamp))
                }
            }
        }

        private fun toggleOBO(enabled: Boolean, timestamp: Long) {
            scope.oboLaunch(OBO_TAG) {
                repository.setUseOBOScheduler(enabled)
                dispatch(Msg.OBOToggled(enabled, timestamp))
            }
        }
    }

    private object ReducerImpl : Reducer<State, Msg> {
        override fun State.reduce(msg: Msg): State {
            return when (msg) {
                is Msg.SettingsLoaded -> copy(useOBO = msg.useOBO)
                is Msg.EffectsPerItemChanged -> {
                    val record = OBOHistoryRecord(OBOHistoryType.SetEffects(msg.count), msg.timestamp)
                    record.applyToState(this)
                }
                is Msg.BlockTimeChanged -> {
                    val record = OBOHistoryRecord(OBOHistoryType.SetBlockTime(msg.ms), msg.timestamp)
                    record.applyToState(this)
                }
                is Msg.Reloaded -> {
                    val record = OBOHistoryRecord(OBOHistoryType.Reload, msg.timestamp)
                    record.applyToState(this)
                }
                is Msg.OBOToggled -> {
                    val record = OBOHistoryRecord(OBOHistoryType.ToggleOBO(msg.enabled), msg.timestamp)
                    record.applyToState(this)
                }
                is Msg.ScrollPositionUpdated -> {
                    val record = OBOHistoryRecord(OBOHistoryType.Scroll(msg.position), msg.timestamp)
                    record.applyToState(this)
                }
            }
        }
    }
}
