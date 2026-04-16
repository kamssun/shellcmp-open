package com.example.archshowcase.presentation.main

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.replayable.Replayable
import kotlinx.serialization.Serializable

@Serializable
sealed interface MainTabHistoryType : JvmSerializable {
    @Serializable
    data class SelectTab(val index: Int) : MainTabHistoryType
}

@Replayable(stateClass = MainTabStore.State::class)
@Serializable
data class MainTabHistoryRecord(
    val type: MainTabHistoryType,
    val timestamp: Long
) : JvmSerializable {

    fun applyToState(prevState: MainTabStore.State): MainTabStore.State = when (type) {
        is MainTabHistoryType.SelectTab -> prevState.copy(
            selectedIndex = type.index,
            history = prevState.appendHistory(this)
        )
    }

    fun toIntent(): Any = when (type) {
        is MainTabHistoryType.SelectTab -> MainTabStore.Intent.SelectTab(type.index)
    }
}
