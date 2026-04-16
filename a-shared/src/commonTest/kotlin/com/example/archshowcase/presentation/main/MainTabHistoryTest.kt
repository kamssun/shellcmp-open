package com.example.archshowcase.presentation.main

import com.example.archshowcase.core.AppConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MainTabHistoryTest {

    private val initialState = MainTabStore.State()

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
    }

    @AfterTest
    fun teardown() {
        AppConfig.enableRestore = false
    }

    @Test
    fun `SelectTab applyToState updates selectedIndex`() {
        val record = MainTabHistoryRecord(MainTabHistoryType.SelectTab(2), timestamp = 100L)
        val newState = record.applyToState(initialState)

        assertEquals(2, newState.selectedIndex)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `SelectTab toIntent creates SelectTab intent`() {
        val record = MainTabHistoryRecord(MainTabHistoryType.SelectTab(3), timestamp = 100L)
        val intent = record.toIntent()
        assertIs<MainTabStore.Intent.SelectTab>(intent)
        assertEquals(3, intent.index)
    }

    @Test
    fun `history accumulates across multiple records`() {
        val r1 = MainTabHistoryRecord(MainTabHistoryType.SelectTab(1), 100L)
        val r2 = MainTabHistoryRecord(MainTabHistoryType.SelectTab(2), 200L)
        val r3 = MainTabHistoryRecord(MainTabHistoryType.SelectTab(0), 300L)

        val s1 = r1.applyToState(initialState)
        val s2 = r2.applyToState(s1)
        val s3 = r3.applyToState(s2)

        assertEquals(3, s3.history.size)
        assertEquals(0, s3.selectedIndex)
    }

    @Test
    fun `hasValidData returns false for empty history`() {
        assertEquals(false, initialState.hasValidData())
    }

    @Test
    fun `hasValidData returns true after recording`() {
        val record = MainTabHistoryRecord(MainTabHistoryType.SelectTab(1), 100L)
        val newState = record.applyToState(initialState)

        assertEquals(true, newState.hasValidData())
    }

    @Test
    fun `createInitialState returns default state`() {
        val fresh = initialState.createInitialState()
        assertEquals(0, fresh.selectedIndex)
        assertEquals(emptyList(), fresh.history)
    }
}
