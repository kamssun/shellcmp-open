package com.example.archshowcase.core.trace.restore

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.Store
import com.example.archshowcase.core.trace.leak.LeakAuditor
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.core.trace.scroll.ScrollRestorableState
import com.example.archshowcase.core.trace.scroll.ScrollRestoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

/**
 * 注册可恢复的 Store
 *
 * 自动处理：
 * 1. 使用 instanceKeeper 管理 Store 生命周期
 * 2. 注册到 RestoreRegistry 进行状态同步
 * 3. 生命周期结束时自动注销
 *
 * @param name Store 名称（用于 TimeTravel 匹配）
 * @param factory Store 工厂函数
 * @param scope 用于状态同步的协程作用域，默认绑定 Component 生命周期
 */
fun <I : Any, S : RestorableState, L : Any> ComponentContext.registerRestorableStore(
    name: String,
    factory: () -> Store<I, S, L>,
    scope: CoroutineScope = coroutineScope()
): Store<I, S, L> {
    val store = instanceKeeper.getStore(factory)

    val disposable = RestoreRegistry.register(name, store, scope)
    LeakAuditor.trackStoreRegister(name)

    lifecycle.doOnDestroy {
        disposable.dispose()
        LeakAuditor.trackStoreUnregister(name)
    }

    return store
}

/**
 * 注册带滚动恢复的 Store
 *
 * 在 [registerRestorableStore] 基础上，额外提供滚动位置恢复。
 *
 * @param name Store 名称
 * @param factory Store 工厂函数
 * @param getItemCount 从 State 获取列表项数量
 * @param isUserScrolling 判断是否用户主动滚动
 * @param scope 用于状态同步的协程作用域，默认绑定 Component 生命周期
 * @return Store 和 滚动恢复事件流
 */
fun <I : Any, S, L : Any> ComponentContext.registerScrollRestorableStore(
    name: String,
    factory: () -> Store<I, S, L>,
    getItemCount: (S) -> Int,
    isUserScrolling: () -> Boolean,
    scope: CoroutineScope = coroutineScope()
): Pair<Store<I, S, L>, SharedFlow<ScrollPosition>> where S : RestorableState, S : ScrollRestorableState {
    val store = registerRestorableStore(name, factory, scope)

    val scrollHelper = ScrollRestoreHelper(scope)
    scrollHelper.observe(
        store = store,
        getItemCount = { getItemCount(store.state) },
        isUserScrolling = isUserScrolling
    )

    return store to scrollHelper.scrollRestoreEvent
}
