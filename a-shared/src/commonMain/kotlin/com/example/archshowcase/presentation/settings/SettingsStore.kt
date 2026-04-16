package com.example.archshowcase.presentation.settings

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.settings.SettingsStore.Intent
import com.example.archshowcase.presentation.settings.SettingsStore.State

/** 设置 key 常量 */
object SettingsKey {
    const val USE_OBO_SCHEDULER = "useOBOScheduler"
}

@VfResolvable
interface SettingsStore : Store<Intent, State, Nothing> {

    sealed interface Intent : JvmSerializable {
        data class SetOBOScheduler(val enabled: Boolean) : Intent
    }

    @CustomState
    data class State(
        val useOBOScheduler: Boolean = true,
        override val history: AppendOnlyHistory<SettingsHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<SettingsHistoryRecord> {
        override fun hasValidData(): Boolean = true
        override fun createInitialState(): ReplayableState<SettingsHistoryRecord> = State()
    }

    sealed interface Action : JvmSerializable {
        data class LoadedSettings(val useOBOScheduler: Boolean) : Action
    }

    sealed interface Msg : JvmSerializable {
        data class SettingsLoaded(val useOBOScheduler: Boolean) : Msg
        data class OBOSchedulerUpdated(val oldValue: Boolean, val newValue: Boolean, val timestamp: Long) : Msg
    }
}
