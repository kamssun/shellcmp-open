package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.arkivanov.mvikotlin.timetravel.export.DefaultTimeTravelExportSerializer
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.export.strategies.TraceExportStrategy
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.tester.TestTraceStore.UserAction

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

    // 构建导出上下文
    val context = ExportContext(
        restorableStates = restorableStates,
        extras = mapOf(
            ExportContext.KEY_TRACE_ACTIONS to traceActions
        )
    )

    val events = mutableListOf<TimeTravelEvent>()

    // 批量生成非时间线 Store 的状态事件（前缀匹配过滤实例化 store name）
    val timelinePrefixes = StoreExportRegistry.getStoreNames()
    val (stateEvents, nextEventId) = RestoreRegistry.generateStateEvents(startEventId = 1L)
    events.addAll(stateEvents.filter { event ->
        timelinePrefixes.none { prefix ->
            event.storeName == prefix || event.storeName.startsWith("$prefix:")
        }
    })

    var eventId = nextEventId

    // 收集所有带时间戳的记录（支持多实例：一个策略可对应多个实例化 store name）
    val allRecords = mutableListOf<TimestampedRecord>()
    StoreExportRegistry.getAll().forEach { strategy ->
        val prefix = strategy.storeName
        val matchingEntries = restorableStates.filter { (name, _) ->
            name == prefix || name.startsWith("$prefix:")
        }
        if (matchingEntries.isEmpty()) {
            allRecords.addAll(strategy.collectTimestampedRecords(strategy.prepareExportState(context)))
        } else {
            matchingEntries.forEach { (instanceName, state) ->
                val records = strategy.collectTimestampedRecords(state)
                if (instanceName != prefix) {
                    // 将 record 的 storeName 重映射为实例名
                    records.mapTo(allRecords) { r ->
                        SimpleRecord((r as SimpleRecord<*>).data, r.timestamp, instanceName)
                    }
                } else {
                    allRecords.addAll(records)
                }
            }
        }
    }
    allRecords.sortBy { it.timestamp }

    // 初始化状态持有器（每个实例独立 holder）
    val stateHolders = mutableMapOf<String, StateHolder>()
    StoreExportRegistry.getAll().forEach { strategy ->
        val prefix = strategy.storeName
        val matchingNames = restorableStates.keys.filter { name ->
            name == prefix || name.startsWith("$prefix:")
        }
        if (matchingNames.isEmpty()) {
            stateHolders[prefix] = strategy.createInitialHolder(context)
        } else {
            matchingNames.forEach { instanceName ->
                stateHolders[instanceName] = strategy.createInitialHolder(context)
            }
        }
    }

    // 按时间顺序生成事件
    allRecords.forEach { record ->
        val strategy = StoreExportRegistry.get(record.storeName) ?: return@forEach
        val holder = stateHolders[record.storeName] ?: return@forEach
        events.addAll(strategy.generateEvents(record, holder) { eventId++ })
    }

    // 构建初始状态（基础名 + 实例名均需提供）
    val initialStates = mutableMapOf<String, Any>()
    val eventStoreNames = events.map { it.storeName }.toSet()
    StoreExportRegistry.getAll().forEach { strategy ->
        val baseInitial = strategy.extractStoreState(strategy.createInitialState())
        initialStates[strategy.storeName] = baseInitial
        eventStoreNames.filter { it.startsWith("${strategy.storeName}:") }.forEach { instanceName ->
            initialStates[instanceName] = baseInitial
        }
    }

    val export = TimeTravelExport(
        recordedEvents = events,
        unusedStoreStates = initialStates
    )

    return when (val result = DefaultTimeTravelExportSerializer.serialize(export)) {
        is TimeTravelExportSerializer.Result.Success -> ExportResult.Success(data = result.data, extension = "tte")
        is TimeTravelExportSerializer.Result.Error -> ExportResult.Error("导出失败: ${result.exception.message}")
    }
}

actual fun serializeTimeTravelExport(export: TimeTravelExport): ExportResult =
    when (val result = DefaultTimeTravelExportSerializer.serialize(export)) {
        is TimeTravelExportSerializer.Result.Success -> ExportResult.Success(data = result.data, extension = "tte")
        is TimeTravelExportSerializer.Result.Error -> ExportResult.Error("序列化失败: ${result.exception.message}")
    }

actual fun deserializeTimeTravelExport(data: ByteArray): TimeTravelExportSerializer.Result<TimeTravelExport> =
    DefaultTimeTravelExportSerializer.deserialize(data)
