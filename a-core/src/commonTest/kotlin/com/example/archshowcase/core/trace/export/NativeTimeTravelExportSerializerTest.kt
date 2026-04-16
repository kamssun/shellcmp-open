package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.export.strategies.TraceExportStrategy
import com.example.archshowcase.core.trace.export.strategies.TraceStateWrapper
import com.example.archshowcase.core.trace.tester.TestTraceStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NativeTimeTravelExportSerializerTest {

    @BeforeTest
    fun setup() {
        StoreExportRegistry.clear()
        StoreExportRegistry.register(TraceExportStrategy)
    }

    @AfterTest
    fun teardown() {
        StoreExportRegistry.clear()
    }

    private fun createTestActions() = listOf(
        TestTraceStore.UserAction(id = 1, name = "click_button", timestamp = 100L),
        TestTraceStore.UserAction(id = 2, name = "scroll_list", timestamp = 200L),
        TestTraceStore.UserAction(id = 3, name = "navigate_back", timestamp = 300L)
    )

    private fun createTestStates(actions: List<TestTraceStore.UserAction>): Map<String, com.example.archshowcase.core.trace.restore.RestorableState> {
        val traceState = TestTraceStore.State(
            actions = actions,
            crashSnapshot = null,
            isRestored = false,
            statusMessage = "已记录 ${actions.size} 个行为",
            importError = null
        )
        return mapOf(
            "TraceStore" to TraceStateWrapper(traceState = traceState, actions = actions)
        )
    }

    @Test
    fun `serialize then deserialize round-trip preserves data`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        // Serialize
        val serializeResult = NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        )
        assertIs<TimeTravelExportSerializer.Result.Success<ByteArray>>(serializeResult)
        val bytes = serializeResult.data

        // Deserialize
        val deserializeResult = NativeTimeTravelExportSerializer.deserialize(bytes)
        assertIs<TimeTravelExportSerializer.Result.Success<TimeTravelExport>>(deserializeResult)
        val export = deserializeResult.data

        // 验证 recordedEvents 不为空
        assertTrue(export.recordedEvents.isNotEmpty(), "recordedEvents should not be empty")
    }

    @Test
    fun `deserialized events contain correct store names`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        val export = (NativeTimeTravelExportSerializer.deserialize(bytes)
            as TimeTravelExportSerializer.Result.Success).data

        // 所有事件都属于 TraceStore
        assertTrue(export.recordedEvents.all { it.storeName == "TraceStore" })
    }

    @Test
    fun `deserialized events contain STATE events with actual Store State`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        val export = (NativeTimeTravelExportSerializer.deserialize(bytes)
            as TimeTravelExportSerializer.Result.Success).data

        // 找 STATE 类型事件
        val stateEvents = export.recordedEvents.filter { it.type == StoreEventType.STATE }
        assertTrue(stateEvents.isNotEmpty(), "Should have STATE events")

        // value 应该是 TestTraceStore.State（MVIKotlin 步进时会 as State 强转）
        stateEvents.forEach { event ->
            assertIs<TestTraceStore.State>(event.value, "STATE event value should be TestTraceStore.State")
        }
    }

    @Test
    fun `unusedStoreStates contain actual Store State not wrapper`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        val export = (NativeTimeTravelExportSerializer.deserialize(bytes)
            as TimeTravelExportSerializer.Result.Success).data

        val traceStoreState = export.unusedStoreStates["TraceStore"]
        assertIs<TestTraceStore.State>(traceStoreState,
            "unusedStoreStates should contain TestTraceStore.State, not TraceStateWrapper")
    }

    @Test
    fun `deserialized events contain INTENT events with actual Intent objects`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        val export = (NativeTimeTravelExportSerializer.deserialize(bytes)
            as TimeTravelExportSerializer.Result.Success).data

        val intentEvents = export.recordedEvents.filter { it.type == StoreEventType.INTENT }
        assertTrue(intentEvents.isNotEmpty(), "Should have INTENT events")

        intentEvents.forEach { event ->
            assertIs<TestTraceStore.Intent.RecordAction>(event.value,
                "INTENT event value should be RecordAction")
        }
    }

    @Test
    fun `event count matches expected pattern per action`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        val export = (NativeTimeTravelExportSerializer.deserialize(bytes)
            as TimeTravelExportSerializer.Result.Success).data

        // TraceExportStrategy.processRecord 生成 Intent + Message + State = 3 events per action
        assertEquals(actions.size * 3, export.recordedEvents.size,
            "Each action should generate Intent + Message + State events")
    }

    @Test
    fun `serialize with empty data returns success with empty records`() {
        val result = NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = emptyList(),
            restorableStates = emptyMap(),
            exportTime = 1000L
        )
        assertIs<TimeTravelExportSerializer.Result.Success<ByteArray>>(result)
    }

    @Test
    fun `deserialize invalid data returns error`() {
        val result = NativeTimeTravelExportSerializer.deserialize("invalid json".encodeToByteArray())
        assertIs<TimeTravelExportSerializer.Result.Error>(result)
    }

    @Test
    fun `ttr format is valid JSON`() {
        val actions = createTestActions()
        val states = createTestStates(actions)

        val bytes = (NativeTimeTravelExportSerializer.serializeFromContext(
            traceActions = actions,
            restorableStates = states,
            exportTime = 1000L
        ) as TimeTravelExportSerializer.Result.Success).data

        val jsonString = bytes.decodeToString()
        // Should be valid JSON containing expected fields
        assertTrue(jsonString.contains("\"version\""), "Should contain version field")
        assertTrue(jsonString.contains("\"traceActions\""), "Should contain traceActions field")
        assertTrue(jsonString.contains("\"storeRecords\""), "Should contain storeRecords field")
    }
}
