package com.example.archshowcase.presentation.demo

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.archshowcase.presentation.demo.detail.DefaultDetailComponent
import com.example.archshowcase.presentation.demo.home.DefaultDemoHomeComponent
import com.example.archshowcase.presentation.navigation.DefaultAppComponentContext
import com.example.archshowcase.presentation.navigation.Navigator
import com.example.archshowcase.presentation.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val baseContext = DefaultComponentContext(lifecycle = lifecycle)

    private val navigatedRoutes = mutableListOf<Route>()
    private val fakeNavigator = object : Navigator {
        override val currentRoute: Route = Route.Home
        override fun push(route: Route) { navigatedRoutes.add(route) }
        override fun pop() {}
        override fun replaceAll(vararg routes: Route) {}
        override fun restore() {}
    }

    private val appContext = DefaultAppComponentContext(baseContext, fakeNavigator)

    @Test
    fun `DetailComponent stores itemId`() {
        val component = DefaultDetailComponent(appContext, itemId = "abc-123")
        assertEquals("abc-123", component.itemId)
    }

    @Test
    fun `DemoHomeComponent onNavigate pushes route`() {
        val component = DefaultDemoHomeComponent(appContext)
        component.onNavigate(Route.NetworkDemo)
        assertEquals(Route.NetworkDemo, navigatedRoutes.last())
    }

    @Test
    fun `DemoHomeComponent onNavigate pushes detail route`() {
        val component = DefaultDemoHomeComponent(appContext)
        component.onNavigate(Route.Detail("item-1"))
        assertEquals(Route.Detail("item-1"), navigatedRoutes.last())
    }
}
