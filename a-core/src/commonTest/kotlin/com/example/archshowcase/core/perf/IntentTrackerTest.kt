package com.example.archshowcase.core.perf

import com.example.archshowcase.core.trace.user.IntentTracker
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntentTrackerTest {

    @AfterTest
    fun cleanup() {
        IntentTracker.clear()
    }

    @Test
    fun `snapshot returns empty list when no intents recorded`() {
        val snapshot = IntentTracker.snapshot()
        assertTrue(snapshot.isEmpty())
    }

    @Test
    fun `snapshot returns immutable copy`() {
        IntentTracker.record("TestStore", "Intent1")
        val snapshot1 = IntentTracker.snapshot()
        IntentTracker.record("TestStore", "Intent2")
        val snapshot2 = IntentTracker.snapshot()

        assertEquals(1, snapshot1.size)
        assertEquals(2, snapshot2.size)
    }

    @Test
    fun `buffer caps at 50 entries`() {
        repeat(60) { i ->
            IntentTracker.record("Store", "Intent$i")
        }
        val snapshot = IntentTracker.snapshot()
        assertEquals(50, snapshot.size)
        assertEquals("Intent10", snapshot.first().intent)
        assertEquals("Intent59", snapshot.last().intent)
    }

    @Test
    fun `snapshot preserves store name and intent`() {
        IntentTracker.record("MyStore", "DoSomething")
        val record = IntentTracker.snapshot().single()
        assertEquals("MyStore", record.store)
        assertEquals("DoSomething", record.intent)
        assertTrue(record.timestamp > 0)
    }
}
