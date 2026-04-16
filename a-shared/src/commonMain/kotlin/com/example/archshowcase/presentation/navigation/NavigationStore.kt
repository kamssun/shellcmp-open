package com.example.archshowcase.presentation.navigation

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.navigation.NavigationStore.Intent
import com.example.archshowcase.presentation.navigation.NavigationStore.State

/**
 * 导航追踪 Store，将 Decompose 导航操作封装为 MVI Intent
 * 实现 TimeTravel 兼容，崩溃时可完整回溯导航链路
 */
@VfResolvable(storeName = "DemoNavigationStore")
interface NavigationStore : Store<Intent, State, Nothing> {

    sealed interface Intent : JvmSerializable {
        data class Push(val route: Route) : Intent
        data object Pop : Intent
        data class BringToFront(val route: Route) : Intent
        data class ReplaceAll(val routes: List<Route>) : Intent
    }

    data class State(
        val stack: List<Route> = listOf(Route.Home),
        override val history: AppendOnlyHistory<NavHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<NavHistoryRecord> {
        val currentRoute: Route get() = stack.lastOrNull() ?: Route.Home
        val stackSize: Int get() = stack.size
        override fun hasValidData() = history.isNotEmpty()
        override fun createInitialState() = State()
    }

    /**
     * 内部消息类型，用于 TimeTravel 导出
     */
    sealed interface Msg : JvmSerializable {
        data class Pushed(val route: Route, val record: NavHistoryRecord) : Msg
        data class Popped(val record: NavHistoryRecord) : Msg
        data class BroughtToFront(val route: Route, val record: NavHistoryRecord) : Msg
        data class ReplacedAll(val routes: List<Route>, val record: NavHistoryRecord) : Msg
    }
}
