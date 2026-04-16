package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.example.archshowcase.core.trace.restore.RestorableState
import kotlinx.serialization.KSerializer

/**
 * 导出上下文，包含导出时需要的所有数据
 */
data class ExportContext(
    val restorableStates: Map<String, RestorableState>,
    val extras: Map<String, Any> = emptyMap()
) {
    inline fun <reified T : RestorableState> getState(key: String): T? =
        restorableStates[key] as? T

    inline fun <reified T : Any> getExtra(key: String): T? =
        extras[key] as? T

    companion object {
        const val KEY_TRACE_ACTIONS = "traceActions"
    }
}

/**
 * Store 导出策略接口，支持多态扩展
 */
interface StoreExportStrategy {
    val storeName: String
    fun collectTimestampedRecords(state: RestorableState): List<TimestampedRecord>
    fun generateEvents(
        record: TimestampedRecord,
        stateHolder: StateHolder,
        nextEventId: () -> Long
    ): List<TimeTravelEvent>
    fun createInitialState(): RestorableState

    /**
     * 从上下文准备导出状态
     * 默认从 restorableStates 获取，子类可覆盖以进行特殊处理
     */
    fun prepareExportState(context: ExportContext): RestorableState =
        context.restorableStates[storeName] ?: createInitialState()

    /**
     * 从上下文创建事件生成的初始 StateHolder
     * 默认使用 createInitialState()，子类可覆盖
     */
    fun createInitialHolder(context: ExportContext): StateHolder =
        StateHolder(createInitialState())

    /**
     * 从策略内部状态提取实际 Store State（用于 TimeTravelEvent）。
     * 默认返回 state 本身；使用包装状态的策略（如 TraceExportStrategy）须覆写。
     */
    fun extractStoreState(state: RestorableState): Any = state

    /**
     * 从反序列化后的 record 提取时间戳（用于跨 Store 按时间交错排序）。
     * 返回 null 表示该策略不支持时间戳提取。
     */
    fun getRecordTimestamp(record: Any): Long? = null

    /**
     * 返回 HistoryRecord 的 KSerializer，用于 .ttr 格式的跨平台序列化。
     * 返回 null 表示该策略不支持 .ttr 序列化。
     */
    fun recordSerializer(): KSerializer<*>? = null
}

/**
 * 带时间戳的记录基类
 */
abstract class TimestampedRecord(val timestamp: Long, val storeName: String)

/**
 * 可变状态持有器，用于在生成事件时追踪状态变化
 */
class StateHolder(var state: RestorableState)

/**
 * 简化的 Record 包装器，用于只有单一 history 字段的 Store
 */
class SimpleRecord<T>(
    val data: T,
    timestamp: Long,
    storeName: String
) : TimestampedRecord(timestamp, storeName)

/**
 * 生成标准的 Intent → Message → State 三元组事件
 */
fun createStandardEvents(
    storeName: String,
    nextEventId: () -> Long,
    intent: Any,
    msg: Any?,
    currentState: Any,
    prevState: Any
): List<TimeTravelEvent> = buildList {
    add(TimeTravelEvent(nextEventId(), storeName, StoreEventType.INTENT, intent, currentState))
    if (msg != null) {
        add(TimeTravelEvent(nextEventId(), storeName, StoreEventType.MESSAGE, msg, currentState))
    }
    add(TimeTravelEvent(nextEventId(), storeName, StoreEventType.STATE, currentState, prevState))
}

/**
 * 简化的导出策略基类，适用于有单一 history 字段的 Store
 *
 * @param S State 类型
 * @param R 原始 record 类型（存储在 State.history 中的类型）
 */
abstract class SimpleExportStrategy<S : RestorableState, R : Any>(
    override val storeName: String,
    private val getHistory: (S) -> List<R>,
    private val getTimestamp: (R) -> Long,
    private val recordKSerializer: KSerializer<R>? = null
) : StoreExportStrategy {

    override fun recordSerializer(): KSerializer<*>? = recordKSerializer

    @Suppress("UNCHECKED_CAST")
    override fun getRecordTimestamp(record: Any): Long? =
        try { getTimestamp(record as R) } catch (_: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    override fun collectTimestampedRecords(state: RestorableState): List<TimestampedRecord> {
        val typedState = state as? S ?: return emptyList()
        return getHistory(typedState).map { SimpleRecord(it, getTimestamp(it), storeName) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun generateEvents(
        record: TimestampedRecord,
        stateHolder: StateHolder,
        nextEventId: () -> Long
    ): List<TimeTravelEvent> {
        val simpleRecord = record as SimpleRecord<R>
        val prevState = stateHolder.state as S
        val (currentState, intent, msg) = processRecord(simpleRecord.data, prevState)
        stateHolder.state = currentState
        val currentEventState = extractStoreState(currentState)
        val prevEventState = extractStoreState(prevState)
        // 使用 record.storeName 以支持实例化 store name（如 "ChatRoomStore:convA"）
        return createStandardEvents(record.storeName, nextEventId, intent, msg, currentEventState, prevEventState)
    }

    /**
     * 处理单条记录，返回 (新状态, Intent, Message)
     * Message 可为 null
     */
    protected abstract fun processRecord(record: R, prevState: S): Triple<S, Any, Any?>
}
