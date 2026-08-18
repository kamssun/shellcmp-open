package com.example.archshowcase.presentation.auth

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.presentation.navigation.Route
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthObserverRouteTest {

    @AfterTest
    fun tearDown() {
        AppConfig.useDemoMode = false
    }

    @Test
    fun `logged in users land on main route by default`() {
        AppConfig.useDemoMode = false

        assertEquals(Route.Main, loggedInLandingRoute())
    }

    @Test
    fun `logged in users land on demo home only in demo mode`() {
        AppConfig.useDemoMode = true

        assertEquals(Route.Home, loggedInLandingRoute())
    }
}
