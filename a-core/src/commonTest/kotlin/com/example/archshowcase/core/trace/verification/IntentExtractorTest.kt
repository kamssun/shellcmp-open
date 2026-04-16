package com.example.archshowcase.core.trace.verification

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntentExtractorTest {

    private fun stateEvent(id: Long, store: String, value: Any = "state") =
        TimeTravelEvent(id, store, StoreEventType.STATE, value, value)

    private fun intentEvent(id: Long, store: String, value: Any) =
        TimeTravelEvent(id, store, StoreEventType.INTENT, value, "state")

    // ─── extractDiff ─────────────────────────────────────────────

    @Test
    fun `extractDiff returns only new INTENT events from exportB`() {
        val exportA = TimeTravelExport(
            recordedEvents = listOf(
                intentEvent(1, "NavStore", "InitialNav"),
                stateEvent(2, "NavStore"),
            ),
            unusedStoreStates = emptyMap()
        )

        val exportB = TimeTravelExport(
            recordedEvents = listOf(
                // 来自 A 的事件（id <= 2）
                intentEvent(1, "NavStore", "InitialNav"),
                stateEvent(2, "NavStore"),
                // A→B 新增事件（id > 2）
                intentEvent(3, "NavStore", "Push(ImageDemo)"),
                stateEvent(4, "NavStore"),
                intentEvent(5, "ImageStore", "LoadInitial"),
                stateEvent(6, "ImageStore"),
                intentEvent(7, "ImageStore", "UpdateScroll"),
                stateEvent(8, "ImageStore"),
            ),
            unusedStoreStates = emptyMap()
        )

        val result = IntentExtractor.extractDiff(exportA, exportB)

        assertEquals(3, result.size)
        assertEquals("NavStore", result[0].storeName)
        assertEquals("Push(ImageDemo)", result[0].intentValue)
        assertEquals("ImageStore", result[1].storeName)
        assertEquals("LoadInitial", result[1].intentValue)
        assertEquals("ImageStore", result[2].storeName)
        assertEquals("UpdateScroll", result[2].intentValue)
    }

    @Test
    fun `extractDiff returns empty when no new events`() {
        val events = listOf(
            intentEvent(1, "Store", "Intent1"),
            stateEvent(2, "Store"),
        )
        val exportA = TimeTravelExport(recordedEvents = events, unusedStoreStates = emptyMap())
        val exportB = TimeTravelExport(recordedEvents = events, unusedStoreStates = emptyMap())

        val result = IntentExtractor.extractDiff(exportA, exportB)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractDiff ignores STATE and MESSAGE events`() {
        val exportA = TimeTravelExport(
            recordedEvents = listOf(stateEvent(1, "Store")),
            unusedStoreStates = emptyMap()
        )
        val exportB = TimeTravelExport(
            recordedEvents = listOf(
                stateEvent(1, "Store"),
                stateEvent(2, "Store"),  // STATE, not INTENT
                TimeTravelEvent(3, "Store", StoreEventType.MESSAGE, "msg", "state"),
            ),
            unusedStoreStates = emptyMap()
        )

        val result = IntentExtractor.extractDiff(exportA, exportB)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractDiff works when exportA is empty`() {
        val exportA = TimeTravelExport(recordedEvents = emptyList(), unusedStoreStates = emptyMap())
        val exportB = TimeTravelExport(
            recordedEvents = listOf(
                intentEvent(1, "Store", "Intent1"),
                stateEvent(2, "Store"),
            ),
            unusedStoreStates = emptyMap()
        )

        val result = IntentExtractor.extractDiff(exportA, exportB)
        assertEquals(1, result.size)
        assertEquals("Intent1", result[0].intentValue)
    }

    // ─── extractAll ──────────────────────────────────────────────

    @Test
    fun `extractAll returns all INTENT events`() {
        val export = TimeTravelExport(
            recordedEvents = listOf(
                intentEvent(1, "StoreA", "Intent1"),
                stateEvent(2, "StoreA"),
                intentEvent(3, "StoreB", "Intent2"),
                stateEvent(4, "StoreB"),
            ),
            unusedStoreStates = emptyMap()
        )

        val result = IntentExtractor.extractAll(export)

        assertEquals(2, result.size)
        assertEquals("StoreA", result[0].storeName)
        assertEquals("StoreB", result[1].storeName)
    }

    @Test
    fun `extractAll returns empty for no events`() {
        val export = TimeTravelExport(recordedEvents = emptyList(), unusedStoreStates = emptyMap())
        val result = IntentExtractor.extractAll(export)
        assertTrue(result.isEmpty())
    }

    // ─── StoreIntent 属性 ────────────────────────────────────────

    @Test
    fun `StoreIntent preserves timestamp from event id`() {
        val exportA = TimeTravelExport(recordedEvents = emptyList(), unusedStoreStates = emptyMap())
        val exportB = TimeTravelExport(
            recordedEvents = listOf(intentEvent(42, "Store", "Intent")),
            unusedStoreStates = emptyMap()
        )

        val result = IntentExtractor.extractDiff(exportA, exportB)
        assertEquals(42L, result[0].timestamp)
    }
}
