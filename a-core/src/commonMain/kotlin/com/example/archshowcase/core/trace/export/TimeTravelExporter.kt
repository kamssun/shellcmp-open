package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.tester.TestTraceStore.UserAction

/**
 * 导出结果
 */
sealed class ExportResult {
    data class Success(val data: ByteArray, val extension: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

/**
 * 导入结果
 */
sealed class ImportResult {
    data class Success(val actions: List<UserAction>) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

/**
 * 导出所有 Store 的事件
 *
 * @param restorableStates 从 RestoreRegistry.getAllSnapshots() 获取
 */
expect fun exportAllStores(
    traceActions: List<UserAction>,
    restorableStates: Map<String, RestorableState>
): ExportResult

/**
 * 序列化 TimeTravelExport 为字节数组（回溯录制用，绕过 exportAllStores 重建逻辑）
 */
expect fun serializeTimeTravelExport(export: TimeTravelExport): ExportResult

/**
 * 反序列化 TimeTravel 导出数据
 */
expect fun deserializeTimeTravelExport(data: ByteArray): TimeTravelExportSerializer.Result<TimeTravelExport>
