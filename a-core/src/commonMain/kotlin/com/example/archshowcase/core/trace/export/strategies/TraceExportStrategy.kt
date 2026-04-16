package com.example.archshowcase.core.trace.export.strategies

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.export.ExportContext
import com.example.archshowcase.core.trace.export.SimpleExportStrategy
import com.example.archshowcase.core.trace.export.StateHolder
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.tester.TestTraceStore

private const val STORE_NAME = "TraceStore"

private data class HistoryRecordedMsg(val action: TestTraceStore.UserAction) : JvmSerializable

/**
 * TraceStore 导出策略的状态包装器
 */
data class TraceStateWrapper(
    val traceState: TestTraceStore.State,
    val actions: List<TestTraceStore.UserAction>
) : RestorableState {
    override fun hasValidData() = actions.isNotEmpty()
}

/**
 * TraceStore 导出策略
 * 需要特殊处理：使用 TraceStateWrapper 包装外部传入的 actions
 */
object TraceExportStrategy : SimpleExportStrategy<TraceStateWrapper, TestTraceStore.UserAction>(
    storeName = STORE_NAME,
    getHistory = { it.actions },
    getTimestamp = { it.timestamp },
    recordKSerializer = TestTraceStore.UserAction.serializer()
) {
    private val defaultTraceState = TestTraceStore.State(
        actions = emptyList(),
        crashSnapshot = null,
        isRestored = false,
        statusMessage = "等待记录",
        importError = null
    )

    override fun extractStoreState(state: RestorableState): Any =
        (state as TraceStateWrapper).traceState

    override fun createInitialState(): RestorableState = TraceStateWrapper(
        traceState = defaultTraceState,
        actions = emptyList()
    )

    @Suppress("UNCHECKED_CAST")
    override fun prepareExportState(context: ExportContext): RestorableState {
        val actions = context.getExtra<List<TestTraceStore.UserAction>>(ExportContext.KEY_TRACE_ACTIONS) ?: emptyList()
        return TraceStateWrapper(traceState = defaultTraceState, actions = actions)
    }

    @Suppress("UNCHECKED_CAST")
    override fun createInitialHolder(context: ExportContext): StateHolder {
        val actions = context.getExtra<List<TestTraceStore.UserAction>>(ExportContext.KEY_TRACE_ACTIONS) ?: emptyList()
        return StateHolder(TraceStateWrapper(traceState = defaultTraceState, actions = actions))
    }

    override fun processRecord(
        record: TestTraceStore.UserAction,
        prevState: TraceStateWrapper
    ): Triple<TraceStateWrapper, Any, Any?> {
        val newActions = prevState.traceState.actions + record
        val currentTraceState = TestTraceStore.State(
            actions = newActions,
            crashSnapshot = null,
            isRestored = false,
            statusMessage = "已记录 ${newActions.size} 个行为",
            importError = null
        )
        val currentState = TraceStateWrapper(currentTraceState, prevState.actions)
        val intent = TestTraceStore.Intent.RecordAction(record)
        val msg = HistoryRecordedMsg(record)
        return Triple(currentState, intent, msg)
    }
}
