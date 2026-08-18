package com.example.archshowcase.presentation.root

import com.example.archshowcase.presentation.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RootInitialStackTest {

    @Test
    fun `clean default home stack uses logged-in landing route`() {
        assertEquals(
            expected = listOf(Route.Main),
            actual = resolveRootInitialStack(
                storeStack = listOf(Route.Home),
                initialRoute = Route.Main,
                isRestoring = false,
            )
        )
    }

    @Test
    fun `restored home stack stays on demo container`() {
        assertEquals(
            expected = listOf(Route.Home),
            actual = resolveRootInitialStack(
                storeStack = listOf(Route.Home),
                initialRoute = Route.Main,
                isRestoring = true,
            )
        )
    }

    @Test
    fun `non-home restored stack is preserved`() {
        val stack = listOf(Route.Home, Route.Settings)

        assertEquals(
            expected = stack,
            actual = resolveRootInitialStack(
                storeStack = stack,
                initialRoute = Route.Main,
                isRestoring = false,
            )
        )
    }

    @Test
    fun `replace clean home stack only outside restore mode`() {
        assertTrue(
            shouldReplaceRootInitialStack(
                storeStack = listOf(Route.Home),
                initialRoute = Route.Main,
                isRestoring = false,
            )
        )
        assertFalse(
            shouldReplaceRootInitialStack(
                storeStack = listOf(Route.Home),
                initialRoute = Route.Main,
                isRestoring = true,
            )
        )
    }
}
