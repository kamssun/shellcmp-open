package com.example.archshowcase.presentation.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppComponentContextTest {

    private val fakeNavigator = object : Navigator {
        override val currentRoute: Route = Route.Home
        override fun push(route: Route) {}
        override fun pop() {}
        override fun replaceAll(vararg routes: Route) {}
        override fun restore() {}
    }

    @Test
    fun `navigator is passed through context`() {
        val base = DefaultComponentContext(lifecycle = LifecycleRegistry())
        val appCtx = DefaultAppComponentContext(base, fakeNavigator)
        assertSame(fakeNavigator, appCtx.navigator)
    }

    @Test
    fun `ComponentContext delegation works`() {
        val lifecycle = LifecycleRegistry()
        val base = DefaultComponentContext(lifecycle = lifecycle)
        val appCtx = DefaultAppComponentContext(base, fakeNavigator)
        // stateKeeper and instanceKeeper are delegated from base
        assertSame(base.stateKeeper, appCtx.stateKeeper)
        assertSame(base.instanceKeeper, appCtx.instanceKeeper)
    }

    @Test
    fun `different navigators for different levels`() {
        val rootNav = fakeNavigator
        val demoNav = object : Navigator {
            override val currentRoute: Route = Route.NetworkDemo
            override fun push(route: Route) {}
            override fun pop() {}
            override fun replaceAll(vararg routes: Route) {}
            override fun restore() {}
        }

        val base = DefaultComponentContext(lifecycle = LifecycleRegistry())
        val rootCtx = DefaultAppComponentContext(base, rootNav)
        val demoCtx = DefaultAppComponentContext(base, demoNav)

        assertEquals(Route.Home, rootCtx.navigator.currentRoute)
        assertEquals(Route.NetworkDemo, demoCtx.navigator.currentRoute)
    }
}
