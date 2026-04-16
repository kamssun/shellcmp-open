package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.example.archshowcase.core.trace.restore.RestorableState
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoreExportRegistryTest {

    @AfterTest
    fun teardown() {
        StoreExportRegistry.clear()
    }

    @Test
    fun `registry starts empty`() {
        assertTrue(StoreExportRegistry.getAll().isEmpty())
        assertTrue(StoreExportRegistry.getStoreNames().isEmpty())
    }

    @Test
    fun `register and get strategy`() {
        val strategy = FakeStrategy("TestStore")
        StoreExportRegistry.register(strategy)

        assertEquals(strategy, StoreExportRegistry.get("TestStore"))
    }

    @Test
    fun `get returns null for unregistered store`() {
        assertNull(StoreExportRegistry.get("NonExistent"))
    }

    @Test
    fun `getAll returns all registered strategies`() {
        StoreExportRegistry.register(FakeStrategy("Store1"))
        StoreExportRegistry.register(FakeStrategy("Store2"))

        assertEquals(2, StoreExportRegistry.getAll().size)
    }

    @Test
    fun `getStoreNames returns all names`() {
        StoreExportRegistry.register(FakeStrategy("A"))
        StoreExportRegistry.register(FakeStrategy("B"))

        assertEquals(setOf("A", "B"), StoreExportRegistry.getStoreNames())
    }

    @Test
    fun `clear removes all strategies`() {
        StoreExportRegistry.register(FakeStrategy("X"))
        StoreExportRegistry.clear()

        assertTrue(StoreExportRegistry.getAll().isEmpty())
    }

    @Test
    fun `runExternalRegistrar invokes callback`() {
        var called = false
        StoreExportRegistry.setExternalRegistrar { called = true }
        StoreExportRegistry.runExternalRegistrar()

        assertTrue(called)
    }

    @Test
    fun `runExternalRegistrar does nothing when not set`() {
        StoreExportRegistry.runExternalRegistrar()
    }

    @Test
    fun `register overwrites existing strategy`() {
        StoreExportRegistry.register(FakeStrategy("S"))
        StoreExportRegistry.register(FakeStrategy("S"))

        assertEquals(1, StoreExportRegistry.getAll().size)
    }
}

private data class FakeState(val data: String = "") : RestorableState {
    override fun hasValidData() = false
}

private class FakeStrategy(override val storeName: String) : StoreExportStrategy {
    override fun collectTimestampedRecords(state: RestorableState) = emptyList<TimestampedRecord>()
    override fun generateEvents(
        record: TimestampedRecord,
        stateHolder: StateHolder,
        nextEventId: () -> Long
    ) = emptyList<TimeTravelEvent>()
    override fun createInitialState(): RestorableState = FakeState()
}
