package com.example.archshowcase.presentation.navigation

import com.example.archshowcase.core.AppConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavHistoryTest {

    private val initialState = NavigationStore.State()

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
    }

    @AfterTest
    fun teardown() {
        AppConfig.enableRestore = false
    }

    @Test
    fun `Push applyToState adds route to stack`() {
        val record = NavHistoryRecord(NavHistoryType.Push("NetworkDemo"), timestamp = 100L)
        val newState = record.applyToState(initialState)

        assertEquals(2, newState.stackSize)
        assertEquals(Route.NetworkDemo, newState.currentRoute)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `Pop applyToState removes last route`() {
        val pushed = initialState.copy(
            stack = listOf(Route.Home, Route.NetworkDemo)
        )
        val record = NavHistoryRecord(NavHistoryType.Pop("NetworkDemo"), timestamp = 200L)
        val newState = record.applyToState(pushed)

        assertEquals(1, newState.stackSize)
        assertEquals(Route.Home, newState.currentRoute)
    }

    @Test
    fun `Pop applyToState does not remove last remaining item`() {
        val record = NavHistoryRecord(NavHistoryType.Pop("Home"), timestamp = 300L)
        val newState = record.applyToState(initialState)

        assertEquals(1, newState.stackSize)
        assertEquals(Route.Home, newState.currentRoute)
    }

    @Test
    fun `BringToFront applyToState moves route to top`() {
        val state = initialState.copy(
            stack = listOf(Route.Home, Route.NetworkDemo, Route.ImageDemo)
        )
        val record = NavHistoryRecord(NavHistoryType.BringToFront("NetworkDemo"), timestamp = 400L)
        val newState = record.applyToState(state)

        assertEquals(Route.NetworkDemo, newState.currentRoute)
        assertEquals(3, newState.stackSize)
    }

    @Test
    fun `ReplaceAll applyToState replaces entire stack`() {
        val record = NavHistoryRecord(
            NavHistoryType.ReplaceAll("Login,Home"),
            timestamp = 500L
        )
        val newState = record.applyToState(initialState)

        assertEquals(2, newState.stackSize)
        assertEquals(Route.Home, newState.currentRoute)
        assertEquals(Route.Login, newState.stack.first())
    }

    @Test
    fun `ReplaceAll with empty routes defaults to Home`() {
        val record = NavHistoryRecord(NavHistoryType.ReplaceAll(""), timestamp = 600L)
        val newState = record.applyToState(initialState)

        assertEquals(1, newState.stackSize)
        assertEquals(Route.Home, newState.currentRoute)
    }

    @Test
    fun `Push toIntent creates Push intent`() {
        val record = NavHistoryRecord(NavHistoryType.Push("NetworkDemo"), timestamp = 100L)
        val intent = record.toIntent()
        assertIs<NavigationStore.Intent.Push>(intent)
        assertEquals(Route.NetworkDemo, intent.route)
    }

    @Test
    fun `Pop toIntent creates Pop intent`() {
        val record = NavHistoryRecord(NavHistoryType.Pop("Home"), timestamp = 200L)
        assertIs<NavigationStore.Intent.Pop>(record.toIntent())
    }

    @Test
    fun `BringToFront toIntent creates BringToFront intent`() {
        val record = NavHistoryRecord(NavHistoryType.BringToFront("Settings"), timestamp = 300L)
        val intent = record.toIntent()
        assertIs<NavigationStore.Intent.BringToFront>(intent)
        assertEquals(Route.Settings, intent.route)
    }

    @Test
    fun `ReplaceAll toIntent creates ReplaceAll intent`() {
        val record = NavHistoryRecord(NavHistoryType.ReplaceAll("Home,Settings"), timestamp = 400L)
        val intent = record.toIntent()
        assertIs<NavigationStore.Intent.ReplaceAll>(intent)
        assertEquals(2, intent.routes.size)
    }

    @Test
    fun `history accumulates in state`() {
        val r1 = NavHistoryRecord(NavHistoryType.Push("NetworkDemo"), 100L)
        val r2 = NavHistoryRecord(NavHistoryType.Push("ImageDemo"), 200L)

        val s1 = r1.applyToState(initialState)
        val s2 = r2.applyToState(s1)

        assertEquals(2, s2.history.size)
    }
}
