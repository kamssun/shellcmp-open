package com.example.archshowcase.presentation.demo.obo

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OBOHistoryTest {

    private val initialState = OBODemoStore.State()

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
    }

    @AfterTest
    fun teardown() {
        AppConfig.enableRestore = false
    }

    @Test
    fun `SetEffects applyToState updates effectsPerItem`() {
        val record = OBOHistoryRecord(OBOHistoryType.SetEffects(20), timestamp = 100L)
        val newState = record.applyToState(initialState)

        assertEquals(20, newState.effectsPerItem)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `SetBlockTime applyToState updates blockTimeMs`() {
        val record = OBOHistoryRecord(OBOHistoryType.SetBlockTime(5), timestamp = 200L)
        val newState = record.applyToState(initialState)

        assertEquals(5, newState.blockTimeMs)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `Reload applyToState increments reloadTrigger`() {
        val record = OBOHistoryRecord(OBOHistoryType.Reload, timestamp = 300L)
        val newState = record.applyToState(initialState)

        assertEquals(1, newState.reloadTrigger)
    }

    @Test
    fun `ToggleOBO applyToState updates useOBO`() {
        val record = OBOHistoryRecord(OBOHistoryType.ToggleOBO(false), timestamp = 400L)
        val newState = record.applyToState(initialState)

        assertEquals(false, newState.useOBO)
    }

    @Test
    fun `Scroll applyToState updates scrollPosition`() {
        val pos = ScrollPosition(firstVisibleIndex = 5, offset = 100)
        val record = OBOHistoryRecord(OBOHistoryType.Scroll(pos), timestamp = 500L)
        val newState = record.applyToState(initialState)

        assertEquals(5, newState.scrollPosition.firstVisibleIndex)
        assertEquals(100, newState.scrollPosition.offset)
    }

    @Test
    fun `SetEffects toIntent`() {
        val record = OBOHistoryRecord(OBOHistoryType.SetEffects(15), timestamp = 100L)
        val intent = record.toIntent()
        assertIs<OBODemoStore.Intent.SetEffectsPerItem>(intent)
        assertEquals(15, intent.count)
    }

    @Test
    fun `SetBlockTime toIntent`() {
        val record = OBOHistoryRecord(OBOHistoryType.SetBlockTime(10), timestamp = 200L)
        val intent = record.toIntent()
        assertIs<OBODemoStore.Intent.SetBlockTime>(intent)
        assertEquals(10, intent.ms)
    }

    @Test
    fun `Reload toIntent`() {
        val record = OBOHistoryRecord(OBOHistoryType.Reload, timestamp = 300L)
        assertIs<OBODemoStore.Intent.Reload>(record.toIntent())
    }

    @Test
    fun `ToggleOBO toIntent`() {
        val record = OBOHistoryRecord(OBOHistoryType.ToggleOBO(true), timestamp = 400L)
        val intent = record.toIntent()
        assertIs<OBODemoStore.Intent.ToggleOBO>(intent)
        assertEquals(true, intent.enabled)
    }

    @Test
    fun `Scroll toIntent`() {
        val pos = ScrollPosition(3, 50)
        val record = OBOHistoryRecord(OBOHistoryType.Scroll(pos), timestamp = 500L)
        val intent = record.toIntent()
        assertIs<OBODemoStore.Intent.UpdateScrollPosition>(intent)
        assertEquals(3, intent.firstVisibleIndex)
        assertEquals(50, intent.offset)
    }

    @Test
    fun `history accumulates across multiple records`() {
        val r1 = OBOHistoryRecord(OBOHistoryType.SetEffects(5), 100L)
        val r2 = OBOHistoryRecord(OBOHistoryType.SetBlockTime(1), 200L)
        val r3 = OBOHistoryRecord(OBOHistoryType.Reload, 300L)

        val s1 = r1.applyToState(initialState)
        val s2 = r2.applyToState(s1)
        val s3 = r3.applyToState(s2)

        assertEquals(3, s3.history.size)
        assertEquals(5, s3.effectsPerItem)
        assertEquals(1, s3.blockTimeMs)
        assertEquals(1, s3.reloadTrigger)
    }
}
