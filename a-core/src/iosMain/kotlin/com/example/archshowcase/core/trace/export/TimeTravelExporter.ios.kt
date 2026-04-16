package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.export.strategies.TraceExportStrategy
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.tester.TestTraceStore.UserAction
import kotlin.time.Clock

/**
 * 初始化导出策略注册表
 */
private var strategiesInitialized = false

private fun initStrategies() {
    if (!strategiesInitialized) {
        strategiesInitialized = true
        StoreExportRegistry.register(TraceExportStrategy)
        StoreExportRegistry.runExternalRegistrar()
    }
}

actual fun exportAllStores(
    traceActions: List<UserAction>,
    restorableStates: Map<String, RestorableState>
): ExportResult {
    initStrategies()

    if (traceActions.isEmpty() && restorableStates.values.none { it.hasValidData() }) {
        return ExportResult.Error("没有可导出的数据")
    }

    return when (val result = NativeTimeTravelExportSerializer.serializeFromContext(
        traceActions = traceActions,
        restorableStates = restorableStates,
        exportTime = Clock.System.now().toEpochMilliseconds()
    )) {
        is TimeTravelExportSerializer.Result.Success ->
            ExportResult.Success(data = result.data, extension = "ttr")
        is TimeTravelExportSerializer.Result.Error ->
            ExportResult.Error("导出失败: ${result.exception.message}")
    }
}

actual fun serializeTimeTravelExport(export: TimeTravelExport): ExportResult =
    ExportResult.Error("iOS 暂不支持回溯模式 TTE 序列化")

actual fun deserializeTimeTravelExport(data: ByteArray): TimeTravelExportSerializer.Result<TimeTravelExport> =
    NativeTimeTravelExportSerializer.deserialize(data)
