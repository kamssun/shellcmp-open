package com.example.archshowcase.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RouteTest {

    @Test
    fun `round trip serialization for all routes`() {
        val routes = listOf(
            Route.Home, Route.Login, Route.EmailLogin, Route.NetworkDemo,
            Route.ImageDemo, Route.AdaptiveDemo, Route.CrashDemo,
            Route.Settings, Route.OBODemo, Route.Live, Route.Payment,
            Route.Detail("test-id")
        )
        routes.forEach { route ->
            assertEquals(route, Route.fromSerialName(route.serialName))
        }
    }

    @Test
    fun `fromSerialName parses Detail route`() {
        val route = Route.fromSerialName("Detail(item-42)")
        assertIs<Route.Detail>(route)
        assertEquals("item-42", route.id)
    }

    @Test
    fun `fromSerialName returns Home for unknown name`() {
        assertEquals(Route.Home, Route.fromSerialName("Unknown"))
    }
}
