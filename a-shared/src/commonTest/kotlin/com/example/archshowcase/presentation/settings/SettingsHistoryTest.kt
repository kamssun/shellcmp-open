package com.example.archshowcase.presentation.settings

import com.example.archshowcase.core.AppConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SettingsHistoryTest {

    private val initialState = SettingsStore.State()

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
    }

    @AfterTest
    fun teardown() {
        AppConfig.enableRestore = false
    }

    @Test
    fun `SetOBOScheduler applyToState disables scheduler`() {
        val record = SettingsHistoryRecord(
            SettingsHistoryType.SetOBOScheduler(false),
            timestamp = 100L
        )
        val newState = record.applyToState(initialState)

        assertEquals(false, newState.useOBOScheduler)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `SetOBOScheduler applyToState enables scheduler`() {
        val disabled = initialState.copy(useOBOScheduler = false)
        val record = SettingsHistoryRecord(
            SettingsHistoryType.SetOBOScheduler(true),
            timestamp = 200L
        )
        val newState = record.applyToState(disabled)

        assertEquals(true, newState.useOBOScheduler)
    }

    @Test
    fun `toIntent creates SetOBOScheduler intent`() {
        val record = SettingsHistoryRecord(
            SettingsHistoryType.SetOBOScheduler(false),
            timestamp = 100L
        )
        val intent = record.toIntent()
        assertIs<SettingsStore.Intent.SetOBOScheduler>(intent)
        assertEquals(false, intent.enabled)
    }

    @Test
    fun `history accumulates`() {
        val r1 = SettingsHistoryRecord(SettingsHistoryType.SetOBOScheduler(false), 100L)
        val r2 = SettingsHistoryRecord(SettingsHistoryType.SetOBOScheduler(true), 200L)

        val s1 = r1.applyToState(initialState)
        val s2 = r2.applyToState(s1)

        assertEquals(2, s2.history.size)
        assertEquals(true, s2.useOBOScheduler)
    }
}
