package com.example.archshowcase.presentation.navigation

import com.arkivanov.decompose.ComponentContext

/**
 * 导航器接口，子组件通过 [AppComponentContext] 获取
 */
interface Navigator {
    val currentRoute: Route
    fun push(route: Route)
    fun pop()
    fun replaceAll(vararg routes: Route)
    fun restore()
}

/**
 * 携带 [Navigator] 的 ComponentContext，由父组件在 createChild 中包装注入。
 */
interface AppComponentContext : ComponentContext {
    val navigator: Navigator
    fun onBack() = navigator.pop()
}

class DefaultAppComponentContext(
    componentContext: ComponentContext,
    override val navigator: Navigator,
) : AppComponentContext, ComponentContext by componentContext
