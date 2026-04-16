package com.example.archshowcase.core.trace.verification

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import kotlinx.serialization.Serializable

/**
 * 从 TTE 事件差集提取 Intent 序列
 *
 * @property storeName Intent 所属的 Store 名
 * @property intentValue Intent 对象（运行时类型）
 * @property timestamp 事件时间戳（来自 TimeTravelEvent.id）
 */
data class StoreIntent(
    val storeName: String,
    val intentValue: Any,
    val timestamp: Long
)

/**
 * 可序列化的 Intent 描述（用于 intents.json）
 */
@Serializable
data class IntentDescriptor(
    val store: String,
    val intentType: String,
    val params: Map<String, String> = emptyMap(),
    val delayAfterMs: Long = 0,
    val note: String = ""
)

/**
 * Intent 序列提取器
 *
 * 从两个 TTE（开始状态 A 和结束状态 B）的事件差集中提取 INTENT 事件。
 * 过滤规则：TTE-B 中 id > TTE-A 最大 id 的 INTENT 事件。
 */
object IntentExtractor {

    /**
     * 从两个 TimeTravelExport 中提取 Intent 差集
     *
     * @param exportA 开始状态的 TTE（状态 A）
     * @param exportB 结束状态的 TTE（状态 B，包含 A→B 之间的所有事件）
     * @return A→B 之间新增的 INTENT 事件列表
     */
    fun extractDiff(
        exportA: TimeTravelExport,
        exportB: TimeTravelExport
    ): List<StoreIntent> {
        val maxIdA = exportA.recordedEvents.maxOfOrNull { it.id } ?: 0L

        return exportB.recordedEvents
            .filter { it.id > maxIdA && it.type == StoreEventType.INTENT }
            .map { event ->
                StoreIntent(
                    storeName = event.storeName,
                    intentValue = event.value,
                    timestamp = event.id
                )
            }
    }

    /**
     * 仅从 TTE-B 中提取所有 INTENT 事件（TTE-A 为空的场景）
     */
    fun extractAll(export: TimeTravelExport): List<StoreIntent> {
        return export.recordedEvents
            .filter { it.type == StoreEventType.INTENT }
            .map { event ->
                StoreIntent(
                    storeName = event.storeName,
                    intentValue = event.value,
                    timestamp = event.id
                )
            }
    }
}
