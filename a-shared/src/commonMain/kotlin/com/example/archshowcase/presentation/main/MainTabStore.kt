package com.example.archshowcase.presentation.main

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.main.MainTabStore.Intent
import com.example.archshowcase.presentation.main.MainTabStore.State

@VfResolvable
interface MainTabStore : Store<Intent, State, Nothing> {

    sealed interface Intent : JvmSerializable {
        data class SelectTab(val index: Int) : Intent
    }

    data class State(
        val selectedIndex: Int = 0,
        override val history: AppendOnlyHistory<MainTabHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<MainTabHistoryRecord> {
        override fun hasValidData() = history.isNotEmpty()
        override fun createInitialState() = State()
    }

    sealed interface Msg : JvmSerializable {
        data class TabSelected(val index: Int, val record: MainTabHistoryRecord) : Msg
    }
}
