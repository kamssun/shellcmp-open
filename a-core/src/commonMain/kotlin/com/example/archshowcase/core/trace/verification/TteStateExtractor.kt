package com.example.archshowcase.core.trace.verification

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.example.archshowcase.core.trace.export.NativeTimeTravelExportSerializer
import com.example.archshowcase.core.trace.export.deserializeTimeTravelExport
import com.example.archshowcase.core.trace.restore.RestorableState

/**
 * TTE 状态提取器
 *
 * 从 TTE（.ttr）文件提取各 Store 的最终 State。
 * 复用 NativeTimeTravelExportSerializer 反序列化 + StoreExportRegistry 重建事件。
 */
object TteStateExtractor {

    /**
     * 从 TTE 字节数据提取各 Store 的最终状态
     *
     * @param tteBytes .ttr 文件的字节内容
     * @return 成功时返回 Map<StoreName, RestorableState>；失败时返回错误
     */
    fun extract(tteBytes: ByteArray): Result<Map<String, RestorableState>> {
        return deserializeTte(tteBytes).map { export ->
            extractFromEvents(export.recordedEvents)
        }
    }

    /**
     * 从已反序列化的 TimeTravelExport 提取状态
     */
    fun extractFromExport(export: TimeTravelExport): Map<String, RestorableState> {
        return extractFromEvents(export.recordedEvents)
    }

    /**
     * 反序列化 TTE 字节数据
     */
    fun deserializeTte(tteBytes: ByteArray): Result<TimeTravelExport> {
        // 先试平台默认解析器（JVM: 二进制 .tte，iOS: JSON .ttr）
        val platformResult = deserializeTimeTravelExport(tteBytes)
        if (platformResult is com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer.Result.Success) {
            return Result.success(platformResult.data)
        }
        // 回退到 NativeTimeTravelExportSerializer（JSON .ttr 格式）
        return when (val result = NativeTimeTravelExportSerializer.deserialize(tteBytes)) {
            is com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer.Result.Success ->
                Result.success(result.data)
            is com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer.Result.Error ->
                Result.failure(result.exception)
        }
    }

    /**
     * 从 TimeTravelEvent 列表中提取每个 Store 的最终 STATE
     */
    private fun extractFromEvents(events: List<TimeTravelEvent>): Map<String, RestorableState> {
        val result = mutableMapOf<String, RestorableState>()

        val storeNames = events.map { it.storeName }.distinct()
        storeNames.forEach { storeName ->
            val lastState = events
                .filter { it.storeName == storeName && it.type == StoreEventType.STATE }
                .lastOrNull()
                ?.value as? RestorableState

            if (lastState?.hasValidData() == true) {
                result[storeName] = lastState
            }
        }

        return result
    }

    /**
     * 提取所有 INTENT 事件（用于 IntentExtractor）
     */
    fun extractIntentEvents(events: List<TimeTravelEvent>): List<TimeTravelEvent> {
        return events.filter { it.type == StoreEventType.INTENT }
    }
}
