package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.example.archshowcase.core.trace.restore.RestorableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private data class TestState(val value: Int = 0) : RestorableState {
    override fun hasValidData() = value > 0
}

class StoreExportStrategyHelpersTest {

    @Test
    fun `createStandardEvents generates intent and state events`() {
        var nextId = 1L
        val events = createStandardEvents(
            storeName = "TestStore",
            nextEventId = { nextId++ },
            intent = "DoSomething",
            msg = null,
            currentState = TestState(1),
            prevState = TestState(0)
        )

        assertEquals(2, events.size)
        assertEquals(StoreEventType.INTENT, events[0].type)
        assertEquals(StoreEventType.STATE, events[1].type)
        assertEquals("TestStore", events[0].storeName)
        assertEquals(1L, events[0].id)
        assertEquals(2L, events[1].id)
    }

    @Test
    fun `createStandardEvents includes message event when msg is not null`() {
        var nextId = 1L
        val events = createStandardEvents(
            storeName = "TestStore",
            nextEventId = { nextId++ },
            intent = "DoSomething",
            msg = "SomeMsg",
            currentState = TestState(1),
            prevState = TestState(0)
        )

        assertEquals(3, events.size)
        assertEquals(StoreEventType.INTENT, events[0].type)
        assertEquals(StoreEventType.MESSAGE, events[1].type)
        assertEquals(StoreEventType.STATE, events[2].type)
    }

    @Test
    fun `StateHolder holds and updates state`() {
        val holder = StateHolder(TestState(0))
        assertEquals(TestState(0), holder.state)

        holder.state = TestState(42)
        assertEquals(TestState(42), holder.state)
    }

    @Test
    fun `SimpleRecord wraps data with timestamp and storeName`() {
        val record = SimpleRecord(data = "hello", timestamp = 12345L, storeName = "Store1")

        assertEquals("hello", record.data)
        assertEquals(12345L, record.timestamp)
        assertEquals("Store1", record.storeName)
    }

    @Test
    fun `SimpleExportStrategy collectTimestampedRecords returns records from state`() {
        val strategy = object : SimpleExportStrategy<TestState, String>(
            storeName = "TestStore",
            getHistory = { listOf("a", "b") },
            getTimestamp = { it.length.toLong() }
        ) {
            override fun processRecord(record: String, prevState: TestState): Triple<TestState, Any, Any?> =
                Triple(prevState.copy(value = prevState.value + 1), "Intent", "Msg")

            override fun createInitialState(): RestorableState = TestState()
        }

        val records = strategy.collectTimestampedRecords(TestState(1))
        assertEquals(2, records.size)
        assertEquals("TestStore", records[0].storeName)
    }

    @Test
    fun `SimpleExportStrategy collectTimestampedRecords returns empty for empty history`() {
        val strategy = object : SimpleExportStrategy<TestState, String>(
            storeName = "TestStore",
            getHistory = { emptyList() },
            getTimestamp = { 0L }
        ) {
            override fun processRecord(record: String, prevState: TestState): Triple<TestState, Any, Any?> =
                Triple(prevState, "I", null)

            override fun createInitialState(): RestorableState = TestState()
        }

        val records = strategy.collectTimestampedRecords(TestState(1))
        assertTrue(records.isEmpty())
    }

    @Test
    fun `SimpleExportStrategy generateEvents creates events and updates stateHolder`() {
        val strategy = object : SimpleExportStrategy<TestState, String>(
            storeName = "TestStore",
            getHistory = { emptyList() },
            getTimestamp = { 0L }
        ) {
            override fun processRecord(record: String, prevState: TestState): Triple<TestState, Any, Any?> =
                Triple(prevState.copy(value = prevState.value + 1), "MyIntent", "MyMsg")

            override fun createInitialState(): RestorableState = TestState()
        }

        val holder = StateHolder(TestState(0))
        var nextId = 1L
        val record = SimpleRecord("test", 100L, "TestStore")

        val events = strategy.generateEvents(record, holder, { nextId++ })

        assertEquals(3, events.size)
        assertEquals(TestState(1), holder.state)
    }

    @Test
    fun `StoreExportStrategy default prepareExportState uses restorableStates`() {
        val strategy = object : StoreExportStrategy {
            override val storeName = "MyStore"
            override fun collectTimestampedRecords(state: RestorableState) = emptyList<TimestampedRecord>()
            override fun generateEvents(
                record: TimestampedRecord, stateHolder: StateHolder, nextEventId: () -> Long
            ) = emptyList<com.arkivanov.mvikotlin.timetravel.TimeTravelEvent>()

            override fun createInitialState(): RestorableState = TestState()
        }

        val state = TestState(5)
        val context = ExportContext(restorableStates = mapOf("MyStore" to state))

        assertEquals(state, strategy.prepareExportState(context))
    }

    @Test
    fun `StoreExportStrategy default prepareExportState falls back to createInitialState`() {
        val strategy = object : StoreExportStrategy {
            override val storeName = "MyStore"
            override fun collectTimestampedRecords(state: RestorableState) = emptyList<TimestampedRecord>()
            override fun generateEvents(
                record: TimestampedRecord, stateHolder: StateHolder, nextEventId: () -> Long
            ) = emptyList<com.arkivanov.mvikotlin.timetravel.TimeTravelEvent>()

            override fun createInitialState(): RestorableState = TestState(99)
        }

        val context = ExportContext(restorableStates = emptyMap())
        val result = strategy.prepareExportState(context)
        assertIs<TestState>(result)
        assertEquals(99, (result as TestState).value)
    }

    @Test
    fun `StoreExportStrategy default createInitialHolder uses createInitialState`() {
        val strategy = object : StoreExportStrategy {
            override val storeName = "MyStore"
            override fun collectTimestampedRecords(state: RestorableState) = emptyList<TimestampedRecord>()
            override fun generateEvents(
                record: TimestampedRecord, stateHolder: StateHolder, nextEventId: () -> Long
            ) = emptyList<com.arkivanov.mvikotlin.timetravel.TimeTravelEvent>()

            override fun createInitialState(): RestorableState = TestState(77)
        }

        val context = ExportContext(restorableStates = emptyMap())
        val holder = strategy.createInitialHolder(context)
        assertEquals(TestState(77), holder.state)
    }
}
