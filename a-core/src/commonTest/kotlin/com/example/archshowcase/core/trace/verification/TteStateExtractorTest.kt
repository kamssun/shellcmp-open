package com.example.archshowcase.core.trace.verification

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.example.archshowcase.core.trace.export.StoreExportRegistry
import com.example.archshowcase.core.trace.export.strategies.TraceExportStrategy
import com.example.archshowcase.core.trace.export.strategies.TraceStateWrapper
import com.example.archshowcase.core.trace.export.NativeTimeTravelExportSerializer
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.tester.TestTraceStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private data class VerTestState(
    val value: Int = 0,
    val label: String = ""
) : RestorableState {
    override fun hasValidData() = label.isNotEmpty()
}

class TteStateExtractorTest {

    @BeforeTest
    fun setup() {
        StoreExportRegistry.clear()
        StoreExportRegistry.register(TraceExportStrategy)
    }

    @AfterTest
    fun teardown() {
        StoreExportRegistry.clear()
    }

    // ─── extractFromExport 直接测试 ─────────────────────────────

    @Test
    fun `extractFromExport returns last STATE for each store`() {
        val stateA1 = VerTestState(value = 1, label = "first")
        val stateA2 = VerTestState(value = 2, label = "second")
        val stateB1 = VerTestState(value = 10, label = "only")

        val events = listOf(
            TimeTravelEvent(1, "StoreA", StoreEventType.STATE, stateA1, stateA1),
            TimeTravelEvent(2, "StoreA", StoreEventType.INTENT, "SomeIntent", stateA1),
            TimeTravelEvent(3, "StoreA", StoreEventType.STATE, stateA2, stateA1),
            TimeTravelEvent(4, "StoreB", StoreEventType.STATE, stateB1, stateB1),
        )

        val export = TimeTravelExport(recordedEvents = events, unusedStoreStates = emptyMap())
        val result = TteStateExtractor.extractFromExport(export)

        assertEquals(2, result.size)
        assertEquals(stateA2, result["StoreA"])
        assertEquals(stateB1, result["StoreB"])
    }

    @Test
    fun `extractFromExport skips stores with non-RestorableState values`() {
        // TestTraceStore.State 没有实现 RestorableState，应被跳过
        val traceState = TestTraceStore.State(actions = emptyList())
        val validState = VerTestState(value = 1, label = "valid")

        val events = listOf(
            TimeTravelEvent(1, "TraceStore", StoreEventType.STATE, traceState, traceState),
            TimeTravelEvent(2, "ValidStore", StoreEventType.STATE, validState, validState),
        )

        val export = TimeTravelExport(recordedEvents = events, unusedStoreStates = emptyMap())
        val result = TteStateExtractor.extractFromExport(export)

        assertEquals(1, result.size)
        assertEquals(validState, result["ValidStore"])
    }

    @Test
    fun `extractFromExport skips invalid states`() {
        val invalid = VerTestState(value = 1, label = "")  // hasValidData() == false

        val events = listOf(
            TimeTravelEvent(1, "StoreA", StoreEventType.STATE, invalid, invalid),
        )

        val export = TimeTravelExport(recordedEvents = events, unusedStoreStates = emptyMap())
        val result = TteStateExtractor.extractFromExport(export)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractFromExport ignores non-STATE events`() {
        val state = VerTestState(value = 1, label = "valid")

        val events = listOf(
            TimeTravelEvent(1, "StoreA", StoreEventType.INTENT, state, state),
            TimeTravelEvent(2, "StoreA", StoreEventType.MESSAGE, state, state),
        )

        val export = TimeTravelExport(recordedEvents = events, unusedStoreStates = emptyMap())
        val result = TteStateExtractor.extractFromExport(export)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractFromExport returns empty map for empty events`() {
        val export = TimeTravelExport(recordedEvents = emptyList(), unusedStoreStates = emptyMap())
        val result = TteStateExtractor.extractFromExport(export)

        assertTrue(result.isEmpty())
    }

    // ─── TTE 序列化往返测试 ──────────────────────────────────────

    @Test
    fun `extract from serialized TTE bytes round-trip`() {
        val actions = listOf(
            TestTraceStore.UserAction(id = 1, name = "click", timestamp = 100L),
            TestTraceStore.UserAction(id = 2, name = "scroll", timestamp = 200L),
        )
        val traceState = TestTraceStore.State(
            actions = actions, crashSnapshot = null,
            isRestored = false, statusMessage = "test", importError = null
        )
        val states = mapOf(
            "TraceStore" to TraceStateWrapper(traceState = traceState, actions = actions)
        )

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        // deserializeTte 应该成功
        val deserResult = TteStateExtractor.deserializeTte(bytes)
        assertTrue(deserResult.isSuccess)
        val export = deserResult.getOrThrow()
        assertTrue(export.recordedEvents.isNotEmpty())
    }

    @Test
    fun `extract returns failure for invalid bytes`() {
        val result = TteStateExtractor.extract("invalid".encodeToByteArray())
        assertTrue(result.isFailure)
    }

    // ─── extractIntentEvents ─────────────────────────────────────

    @Test
    fun `extractIntentEvents filters only INTENT events`() {
        val state = VerTestState(value = 1, label = "valid")
        val events = listOf(
            TimeTravelEvent(1, "Store", StoreEventType.INTENT, "Intent1", state),
            TimeTravelEvent(2, "Store", StoreEventType.MESSAGE, "Msg", state),
            TimeTravelEvent(3, "Store", StoreEventType.STATE, state, state),
            TimeTravelEvent(4, "Store", StoreEventType.INTENT, "Intent2", state),
        )

        val intents = TteStateExtractor.extractIntentEvents(events)

        assertEquals(2, intents.size)
        assertEquals("Intent1", intents[0].value)
        assertEquals("Intent2", intents[1].value)
    }
}
