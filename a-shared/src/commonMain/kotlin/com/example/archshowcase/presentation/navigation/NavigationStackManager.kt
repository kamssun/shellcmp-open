package com.example.archshowcase.presentation.navigation

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.timetravel.TimeTravelState
import com.arkivanov.mvikotlin.timetravel.controller.timeTravelController
import com.example.archshowcase.core.analytics.NavigationActionContext
import com.example.archshowcase.core.analytics.model.NavigationAction
import com.example.archshowcase.core.perf.PerfMonitor
import com.example.archshowcase.core.scheduler.oboLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 封装导航栈管理 + NavigationStore 双向同步逻辑。
 * RootComponent / DemoRootComponent 均委托给此类。
 */
@OptIn(DelicateDecomposeApi::class)
class NavigationStackManager(
    private val store: Store<NavigationStore.Intent, NavigationStore.State, Nothing>,
    private val onRestoreComplete: () -> Unit = {},
) : Navigator {

    companion object {
        private const val OBO_TAG = "NavigationStack"
    }

    val navigation = StackNavigation<Route>()
    private lateinit var childStack: Value<ChildStack<*, *>>
    private var isNavigating = false

    private inline fun withNavigating(block: () -> Unit) {
        isNavigating = true
        try { block() } finally { isNavigating = false }
    }

    // --- Navigator ---

    override val currentRoute: Route
        get() = childStack.value.active.configuration as? Route ?: Route.Home

    override fun push(route: Route) = withNavigating {
        NavigationActionContext.current = NavigationAction.PUSH
        PerfMonitor.notifyTransitionStart(currentRoute.toString(), route.toString())
        store.accept(NavigationStore.Intent.Push(route))
        navigation.push(route)
    }

    override fun pop() = withNavigating {
        NavigationActionContext.current = NavigationAction.POP
        val from = currentRoute.toString()
        store.accept(NavigationStore.Intent.Pop)
        navigation.pop()
        val to = currentRoute.toString()
        if (from != to) PerfMonitor.notifyTransitionStart(from, to)
    }

    override fun replaceAll(vararg routes: Route) {
        if (routes.isEmpty()) return
        withNavigating {
            NavigationActionContext.current = NavigationAction.REPLACE_ALL
            val from = currentRoute.toString()
            store.accept(NavigationStore.Intent.ReplaceAll(routes.toList()))
            navigation.replaceAll(*routes)
            PerfMonitor.notifyTransitionStart(from, routes.last().toString())
        }
    }

    override fun restore() = withNavigating {
        val stack = store.state.stack
        if (stack.isNotEmpty()) {
            navigation.replaceAll(*stack.toTypedArray())
        }
        onRestoreComplete()
    }

    // --- 双向同步 ---

    fun startSync(
        childStack: Value<ChildStack<*, *>>,
        scope: CoroutineScope,
        backHandler: BackHandler? = null,
    ) {
        this.childStack = childStack

        // store → navigation（时间旅行回放时驱动 Decompose）
        scope.oboLaunch(OBO_TAG) {
            store.states
                .map { it.currentRoute }
                .distinctUntilChanged()
                .collect { route ->
                    if (!isNavigating) syncNavigationFromStore(route)
                }
        }

        // 替代 handleBackButton=true：所有返回都走 navManager.pop()，确保 NavigationActionContext 正确设置
        backHandler?.let { handler ->
            val callback = BackCallback(isEnabled = false) { pop() }
            handler.register(callback)
            childStack.subscribe { stack ->
                callback.isEnabled = stack.backStack.isNotEmpty()
            }
        }

        // navigation → store（时间旅行等场景下的兜底同步）
        childStack.subscribe { stack ->
            if (isNavigating || timeTravelController.state.mode != TimeTravelState.Mode.IDLE) return@subscribe
            val current = stack.active.configuration as? Route ?: return@subscribe
            if (current != store.state.currentRoute) {
                withNavigating { store.accept(NavigationStore.Intent.Pop) }
            }
        }
    }

    private fun syncNavigationFromStore(route: Route) {
        val current = childStack.value.active.configuration as? Route ?: return
        if (current != route) {
            navigation.replaceAll(*store.state.stack.toTypedArray())
        }
    }
}
