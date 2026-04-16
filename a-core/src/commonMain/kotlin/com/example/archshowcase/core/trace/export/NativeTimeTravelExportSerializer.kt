package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.tester.TestTraceStore.UserAction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * .ttr 格式的中间模型
 * 存储序列化后的 HistoryRecord 列表，导入时通过 Strategy 重建 TimeTravelEvent
 */
@Serializable
data class TtrExport(
    val version: Int = 1,
    val exportTime: Long,
    val traceActions: List<UserAction> = emptyList(),
    val storeRecords: Map<String, JsonElement> = emptyMap()
)

/**
 * 跨平台 TimeTravelExport 序列化器
 *
 * 序列化: 从 StoreExportStrategy 收集 HistoryRecord → JSON
 * 反序列化: JSON → HistoryRecord → Strategy.processRecord() → 重建 TimeTravelEvent
 */
object NativeTimeTravelExportSerializer : TimeTravelExportSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 序列化：收集各 Store 的 HistoryRecord，打包为 .ttr JSON
     */
    fun serializeFromContext(
        traceActions: List<UserAction>,
        restorableStates: Map<String, RestorableState>,
        exportTime: Long
    ): TimeTravelExportSerializer.Result<ByteArray> = try {
        val context = ExportContext(
            restorableStates = restorableStates,
            extras = mapOf(ExportContext.KEY_TRACE_ACTIONS to traceActions)
        )

        val storeRecords = mutableMapOf<String, JsonElement>()

        StoreExportRegistry.getAll().forEach { strategy ->
            val serializer = strategy.recordSerializer() ?: return@forEach
            @Suppress("UNCHECKED_CAST")
            val typedSerializer = serializer as KSerializer<Any>
            val prefix = strategy.storeName

            // 支持多实例：一个策略对应多个实例化 store name
            val matchingEntries = restorableStates.filter { (name, _) ->
                name == prefix || name.startsWith("$prefix:")
            }
            val entriesToExport = matchingEntries.ifEmpty {
                mapOf(prefix to strategy.prepareExportState(context))
            }

            entriesToExport.forEach { (instanceName, state) ->
                val records = strategy.collectTimestampedRecords(state)
                if (records.isNotEmpty()) {
                    val dataList = records.map { (it as SimpleRecord<*>).data!! }
                    val jsonElement = json.encodeToJsonElement(
                        ListSerializer(typedSerializer),
                        dataList
                    )
                    storeRecords[instanceName] = jsonElement
                }
            }
        }

        val ttrExport = TtrExport(
            exportTime = exportTime,
            traceActions = traceActions,
            storeRecords = storeRecords
        )
        val bytes = json.encodeToString(TtrExport.serializer(), ttrExport).encodeToByteArray()
        TimeTravelExportSerializer.Result.Success(bytes)
    } catch (e: Exception) {
        TimeTravelExportSerializer.Result.Error(e)
    }

    /**
     * 反序列化时的中间条目：一条 record + 它所属的 strategy 和 stateHolder
     */
    private class RecordEntry(
        val record: Any,
        val timestamp: Long,
        val strategy: StoreExportStrategy,
        val stateHolder: StateHolder,
        val instanceName: String
    )

    /**
     * 反序列化：从 .ttr JSON 重建 TimeTravelExport（含实际 Kotlin 对象）
     *
     * 关键：跨 Store 按时间戳交错排序事件，保证回放顺序与用户操作一致。
     * 例：导航→滚动→返回→设置，而非先放完所有滚动再放导航。
     */
    override fun deserialize(data: ByteArray): TimeTravelExportSerializer.Result<TimeTravelExport> = try {
        val ttrExport = json.decodeFromString(TtrExport.serializer(), data.decodeToString())

        var eventId = 1L
        val allEntries = mutableListOf<RecordEntry>()
        val initialStates = mutableMapOf<String, Any>()
        val stateHolders = mutableMapOf<String, StateHolder>()

        // 确保策略已注册
        initStrategies()

        val context = ExportContext(
            restorableStates = emptyMap(),
            extras = mapOf(ExportContext.KEY_TRACE_ACTIONS to ttrExport.traceActions)
        )

        // Step 1: 收集所有 Store 的 records，附带时间戳（支持实例化 store name）
        ttrExport.storeRecords.forEach { (storeName, recordsJson) ->
            val strategy = StoreExportRegistry.get(storeName) ?: return@forEach
            val serializer = strategy.recordSerializer() ?: return@forEach

            @Suppress("UNCHECKED_CAST")
            val typedSerializer = serializer as KSerializer<Any>
            val records = json.decodeFromJsonElement(
                ListSerializer(typedSerializer),
                recordsJson
            )

            val stateHolder = strategy.createInitialHolder(context)
            stateHolders[storeName] = stateHolder
            initialStates[storeName] = strategy.extractStoreState(strategy.createInitialState())

            records.forEach { record ->
                val timestamp = strategy.getRecordTimestamp(record) ?: 0L
                allEntries.add(RecordEntry(record, timestamp, strategy, stateHolder, storeName))
            }
        }

        // Step 2: 跨 Store 按时间戳排序（稳定排序，同时间戳保持原序）
        allEntries.sortBy { it.timestamp }

        // Step 3: 按全局时间顺序生成事件
        val allEvents = mutableListOf<TimeTravelEvent>()
        allEntries.forEach { entry ->
            val timestampedRecord = SimpleRecord(
                data = entry.record,
                timestamp = entry.timestamp,
                storeName = entry.instanceName
            )
            val events = entry.strategy.generateEvents(timestampedRecord, entry.stateHolder) { eventId++ }
            allEvents.addAll(events)
        }

        val export = TimeTravelExport(
            recordedEvents = allEvents,
            unusedStoreStates = initialStates
        )
        TimeTravelExportSerializer.Result.Success(export)
    } catch (e: Exception) {
        TimeTravelExportSerializer.Result.Error(e)
    }

    /**
     * serialize 接口方法（从已有 TimeTravelExport 序列化，不常用于 iOS 场景）
     */
    override fun serialize(export: TimeTravelExport): TimeTravelExportSerializer.Result<ByteArray> {
        // 对于 NativeSerializer，推荐使用 serializeFromContext
        // 此方法作为 TimeTravelExportSerializer 接口的兼容实现
        return TimeTravelExportSerializer.Result.Error(
            UnsupportedOperationException("请使用 serializeFromContext() 代替")
        )
    }

    private fun initStrategies() {
        if (StoreExportRegistry.getAll().isEmpty()) {
            StoreExportRegistry.register(
                com.example.archshowcase.core.trace.export.strategies.TraceExportStrategy
            )
            StoreExportRegistry.runExternalRegistrar()
        }
    }

}
