package com.example.archshowcase.presentation.demo.obo

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollRestorableState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.Intent
import com.example.archshowcase.presentation.demo.obo.OBODemoStore.State

@VfResolvable
interface OBODemoStore : Store<Intent, State, Nothing> {

    sealed interface Intent : JvmSerializable {
        data class SetEffectsPerItem(val count: Int) : Intent
        data class SetBlockTime(val ms: Int) : Intent
        data object Reload : Intent
        data class ToggleOBO(val enabled: Boolean) : Intent
        data class UpdateScrollPosition(val firstVisibleIndex: Int, val offset: Int) : Intent
    }

    @CustomState
    data class State(
        val effectsPerItem: Int = 10,
        val blockTimeMs: Int = 3,
        val useOBO: Boolean = true,
        val reloadTrigger: Int = 0,
        override val history: AppendOnlyHistory<OBOHistoryRecord> = AppendOnlyHistory(),
        override val scrollPosition: ScrollPosition = ScrollPosition()
    ) : ReplayableState<OBOHistoryRecord>, ScrollRestorableState {
        override fun hasValidData() = history.isNotEmpty()
        override fun createInitialState() = State()
    }

    sealed interface Action : JvmSerializable {
        data class LoadedSettings(val useOBO: Boolean) : Action
    }

    sealed interface Msg : JvmSerializable {
        data class SettingsLoaded(val useOBO: Boolean) : Msg
        data class EffectsPerItemChanged(val count: Int, val timestamp: Long) : Msg
        data class BlockTimeChanged(val ms: Int, val timestamp: Long) : Msg
        data class Reloaded(val timestamp: Long) : Msg
        data class OBOToggled(val enabled: Boolean, val timestamp: Long) : Msg
        data class ScrollPositionUpdated(val position: ScrollPosition, val timestamp: Long) : Msg
    }
}
