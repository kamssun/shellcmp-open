package com.example.archshowcase.presentation.navigation

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.replayable.Replayable
import kotlinx.serialization.Serializable

/**
 * Navigation 的 HistoryType
 * 用于回溯系统记录导航操作
 */
@Serializable
sealed interface NavHistoryType : JvmSerializable {
    @Serializable
    data class Push(val route: String) : NavHistoryType
    @Serializable
    data class Pop(val route: String) : NavHistoryType
    @Serializable
    data class BringToFront(val route: String) : NavHistoryType
    @Serializable
    data class ReplaceAll(val routes: String) : NavHistoryType
}

/**
 * Navigation 的 HistoryRecord
 * 记录导航操作历史，用于时间旅行和回溯
 */
@Replayable(stateClass = NavigationStore.State::class)
@Replayable(stateClass = NavigationStore.State::class, storeName = "DemoNavigationStore")
@Serializable
data class NavHistoryRecord(
    val type: NavHistoryType,
    val timestamp: Long
) : JvmSerializable {

    /**
     * 应用此记录到前一个状态，生成新状态
     */
    fun applyToState(prevState: NavigationStore.State): NavigationStore.State {
        val newStack = ArrayDeque(prevState.stack)
        when (type) {
            is NavHistoryType.Push -> newStack.addLast(Route.fromSerialName(type.route))
            is NavHistoryType.Pop -> if (newStack.size > 1) newStack.removeLast()
            is NavHistoryType.BringToFront -> {
                val route = Route.fromSerialName(type.route)
                newStack.removeAll { it::class == route::class }
                newStack.addLast(route)
            }
            is NavHistoryType.ReplaceAll -> {
                newStack.clear()
                val routes = if (type.routes.isEmpty()) listOf(Route.Home)
                else type.routes.split(",").map { Route.fromSerialName(it.trim()) }
                routes.forEach(newStack::addLast)
            }
        }
        return prevState.copy(
            stack = newStack.toList(),
            history = prevState.appendHistory(this)
        )
    }

    /**
     * 转换为 Intent 用于回放
     */
    fun toIntent(): Any = when (type) {
        is NavHistoryType.Push -> NavigationStore.Intent.Push(Route.fromSerialName(type.route))
        is NavHistoryType.Pop -> NavigationStore.Intent.Pop
        is NavHistoryType.BringToFront -> NavigationStore.Intent.BringToFront(Route.fromSerialName(type.route))
        is NavHistoryType.ReplaceAll -> {
            val routes = if (type.routes.isEmpty()) listOf(Route.Home)
            else type.routes.split(",").map { Route.fromSerialName(it.trim()) }
            NavigationStore.Intent.ReplaceAll(routes)
        }
    }
}
