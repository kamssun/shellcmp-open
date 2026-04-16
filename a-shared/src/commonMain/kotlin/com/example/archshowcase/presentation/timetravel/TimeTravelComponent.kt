package com.example.archshowcase.presentation.timetravel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.timetravel.TimeTravelState
import com.arkivanov.mvikotlin.timetravel.controller.timeTravelController
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExportSerializer
import com.example.archshowcase.core.trace.bookmark.DevBookmarkHolder
import com.example.archshowcase.core.trace.export.ExportResult
import com.example.archshowcase.core.trace.export.ImportResult
import com.example.archshowcase.core.trace.export.StoreExportRegistry
import com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
import com.example.archshowcase.core.trace.export.deserializeTimeTravelExport
import com.example.archshowcase.core.trace.export.exportAllStores
import com.example.archshowcase.core.trace.export.registerGeneratedExportStrategies
import com.example.archshowcase.core.trace.export.restoreFromEvents
import com.example.archshowcase.core.trace.export.serializeTimeTravelExport
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.tester.TestTraceStoreFactory
import com.example.archshowcase.core.trace.tester.loadTraceModule
import com.example.archshowcase.core.trace.verification.VfManifest
import com.example.archshowcase.core.trace.verification.VfPackage
import com.example.archshowcase.core.trace.verification.VfPackager
import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.example.archshowcase.core.trace.verification.IntentExtractor
import com.example.archshowcase.core.trace.verification.IntentParamsExtractor
import com.example.archshowcase.core.trace.verification.NetworkRecorderBridge
import com.example.archshowcase.core.trace.verification.StoreIntent
import com.example.archshowcase.core.trace.verification.TteStateExtractor
import com.example.archshowcase.presentation.navigation.AppComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

/**
 * VF 导出状态机
 */
sealed class VfExportState {
    data object Idle : VfExportState()
    data class Recording(
        val startTteBytes: ByteArray,
        val startScreenshot: ByteArray? = null,
        val startEventIndex: Int = -1
    ) : VfExportState()
    data class WaitingForText(val startTteBytes: ByteArray) : VfExportState()
}

interface TimeTravelComponent {
    val timeTravelState: StateFlow<TimeTravelState>
    val vfExportState: StateFlow<VfExportState>
    val isReplaying: Boolean
    val bookmarkExists: StateFlow<Boolean>
    val bookmarkMessage: StateFlow<String?>
    fun onExport(): ExportResult
    fun onImport(data: ByteArray): ImportResult
    fun onBookmarkToggle()
    fun onBookmarkMessageConsumed()
    fun onExportVfStart(screenshotBytes: ByteArray? = null)
    fun onExportVfEnd(verificationText: String, screenshotBytes: ByteArray? = null): Map<String, ByteArray>?
    fun onMoveToStart()
    fun onStepBackward()
    fun onStepForward()
    fun onMoveToEnd()
    fun onCancel()
}

class DefaultTimeTravelComponent(
    context: AppComponentContext,
) : TimeTravelComponent, AppComponentContext by context, KoinComponent {

    init {
        loadTraceModule()
        StoreExportRegistry.setExternalRegistrar(::registerGeneratedExportStrategies)
    }

    private val testTraceStoreFactory: TestTraceStoreFactory by inject()
    private val traceStore = instanceKeeper.getStore { testTraceStoreFactory.create() }

    private val _timeTravelState = MutableStateFlow(timeTravelController.state)
    override val timeTravelState: StateFlow<TimeTravelState> = _timeTravelState.asStateFlow()

    private val _vfExportState = MutableStateFlow<VfExportState>(VfExportState.Idle)
    override val vfExportState: StateFlow<VfExportState> = _vfExportState.asStateFlow()

    private val _bookmarkExists = MutableStateFlow(DevBookmarkHolder.storage?.exists() == true)
    override val bookmarkExists: StateFlow<Boolean> = _bookmarkExists.asStateFlow()

    private val _bookmarkMessage = MutableStateFlow(
        DevBookmarkHolder.pendingMessage.also { DevBookmarkHolder.pendingMessage = null }
    )
    override val bookmarkMessage: StateFlow<String?> = _bookmarkMessage.asStateFlow()

    override val isReplaying: Boolean
        get() = timeTravelController.state.mode != TimeTravelState.Mode.IDLE

    private val timeTravelDisposable = timeTravelController.states(
        observer { state -> _timeTravelState.value = state }
    )

    init {
        lifecycle.doOnDestroy { timeTravelDisposable.dispose() }
    }

    override fun onBookmarkToggle() {
        val storage = DevBookmarkHolder.storage ?: return
        if (storage.exists()) {
            storage.delete()
            _bookmarkExists.value = false
            _bookmarkMessage.value = MSG_BOOKMARK_CLEARED
        } else {
            when (val result = onExport()) {
                is ExportResult.Success -> {
                    if (storage.save(result.data)) {
                        _bookmarkExists.value = true
                        _bookmarkMessage.value = MSG_BOOKMARK_SAVED
                    } else {
                        _bookmarkMessage.value = MSG_BOOKMARK_SAVE_FAILED
                    }
                }
                is ExportResult.Error -> {
                    _bookmarkMessage.value = MSG_BOOKMARK_SAVE_FAILED
                }
            }
        }
    }

    override fun onBookmarkMessageConsumed() {
        _bookmarkMessage.value = null
    }

    override fun onExport(): ExportResult =
        exportAllStores(
            traceActions = traceStore.state.actions,
            restorableStates = RestoreRegistry.getAllSnapshots()
        )

    override fun onExportVfStart(screenshotBytes: ByteArray?) {
        val ctrlState = timeTravelController.state
        val eventIndex = if (isReplaying) ctrlState.selectedEventIndex else -1
        val tteBytes = if (eventIndex >= 0) {
            exportPlaybackTte(eventIndex)
        } else {
            when (val result = onExport()) {
                is ExportResult.Success -> result.data
                is ExportResult.Error -> ByteArray(0)
            }
        }
        _vfExportState.value = VfExportState.Recording(
            startTteBytes = tteBytes,
            startScreenshot = screenshotBytes,
            startEventIndex = eventIndex
        )
        NetworkRecorderBridge.markStart()
    }

    override fun onExportVfEnd(verificationText: String, screenshotBytes: ByteArray?): Map<String, ByteArray>? {
        com.example.archshowcase.core.util.Log.d("VF") { "onExportVfEnd: endScreenshot=${screenshotBytes?.size ?: "null"} bytes" }
        val currentState = _vfExportState.value
        val recordStartTteBytes: ByteArray
        val recordStartScreenshot: ByteArray?
        val startEventIndex: Int
        when (currentState) {
            is VfExportState.Recording -> {
                recordStartTteBytes = currentState.startTteBytes
                recordStartScreenshot = currentState.startScreenshot
                startEventIndex = currentState.startEventIndex
            }
            is VfExportState.WaitingForText -> {
                recordStartTteBytes = currentState.startTteBytes
                recordStartScreenshot = null
                startEventIndex = -1
            }
            is VfExportState.Idle -> return null
        }

        val recordEndTteBytes = if (startEventIndex >= 0) {
            // 回溯模式：从 controller 事件构建 TTE
            val endIdx = timeTravelController.state.selectedEventIndex
            exportPlaybackTte(endIdx)
        } else {
            when (val endResult = onExport()) {
                is ExportResult.Success -> endResult.data
                is ExportResult.Error -> {
                    _vfExportState.value = VfExportState.Idle
                    return null
                }
            }
        }

        // 停止网络录制
        val networkTape = NetworkRecorderBridge.markEnd()

        val vfFiles = try {
            val storeIntents: List<StoreIntent>
            val vfStartTte: ByteArray
            val vfEndTte: ByteArray
            val vfStartScreenshot: ByteArray?
            val vfEndScreenshot: ByteArray?

            if (startEventIndex >= 0) {
                // 回溯模式：从 controller 事件列表按索引范围提取 INTENT
                val playback = extractPlaybackIntents(startEventIndex)
                storeIntents = playback.intents
                if (playback.isBackward) {
                    // 反向步进：VF 始终表示正向状态转换，swap TTE 和截图
                    vfStartTte = recordEndTteBytes
                    vfEndTte = recordStartTteBytes
                    vfStartScreenshot = screenshotBytes
                    vfEndScreenshot = recordStartScreenshot
                } else {
                    vfStartTte = recordStartTteBytes
                    vfEndTte = recordEndTteBytes
                    vfStartScreenshot = recordStartScreenshot
                    vfEndScreenshot = screenshotBytes
                }
            } else {
                // 普通模式：从 TTE-A/B 差集提取
                val exportA = if (recordStartTteBytes.isEmpty()) null
                    else TteStateExtractor.deserializeTte(recordStartTteBytes).getOrNull()
                val exportB = TteStateExtractor.deserializeTte(recordEndTteBytes).getOrThrow()
                storeIntents = if (exportA != null) {
                    IntentExtractor.extractDiff(exportA, exportB)
                } else {
                    IntentExtractor.extractAll(exportB)
                }
                vfStartTte = recordStartTteBytes
                vfEndTte = recordEndTteBytes
                vfStartScreenshot = recordStartScreenshot
                vfEndScreenshot = screenshotBytes
            }

            // G1: 使用 IntentParamsExtractor 提取参数
            val vfIntents = storeIntents.map { si ->
                val (intentType, params) = IntentParamsExtractor.extract(si.storeName, si.intentValue)
                com.example.archshowcase.core.trace.verification.VfIntent(
                    store = si.storeName,
                    intentType = intentType,
                    params = params
                )
            }

            // G3: createdAt 时间戳
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val createdAt = "${localDateTime.year}-${localDateTime.monthNumber.toString().padStart(2, '0')}-${localDateTime.dayOfMonth.toString().padStart(2, '0')}T" +
                "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}"

            val networkTapeConfig = com.example.archshowcase.core.trace.verification.NetworkTapeConfig()

            // covers: 从 VF intents 涉及的 Store 名称自动推导
            val covers = vfIntents.map { it.store }.distinct()

            val manifest = VfManifest(
                name = "vf_export",
                verificationText = verificationText,
                intents = vfIntents,
                createdAt = createdAt,
                networkTape = networkTapeConfig,
                covers = covers
            )

            // G2 + G3: extraFiles（截图 + 网络录制）
            com.example.archshowcase.core.util.Log.d("VF") { "extraFiles: startScreenshot=${vfStartScreenshot?.size ?: "null"}, endScreenshot=${vfEndScreenshot?.size ?: "null"}" }
            val extraFiles = buildMap {
                vfStartScreenshot?.let { put("start_baseline.png", it) }
                vfEndScreenshot?.let { put("end_baseline.png", it) }
                if (networkTape != null && networkTape.recordings.isNotEmpty()) {
                    val tapeJson = Json { prettyPrint = true; encodeDefaults = true }
                    put("network_tape.json", tapeJson.encodeToString(
                        com.example.archshowcase.core.trace.verification.NetworkTape.serializer(),
                        networkTape
                    ).encodeToByteArray())
                }
            }

            VfPackager.pack(VfPackage(manifest, vfStartTte, vfEndTte, extraFiles))
        } catch (e: Exception) {
            com.example.archshowcase.core.util.Log.e("VF") { "VF pack failed: ${e.stackTraceToString()}" }
            null
        }

        _vfExportState.value = VfExportState.Idle
        return vfFiles
    }

    private data class PlaybackExtraction(
        val intents: List<StoreIntent>,
        val isBackward: Boolean
    )

    /**
     * 回溯模式：从 timeTravelController 事件列表中提取 INTENT 事件。
     * 始终提取 min→max 范围（正向），通过 isBackward 标记方向，由调用方决定是否 swap TTE。
     */
    private fun extractPlaybackIntents(startEventIndex: Int): PlaybackExtraction {
        val controllerState = timeTravelController.state
        val endEventIndex = controllerState.selectedEventIndex
        val events = controllerState.events

        val isBackward = endEventIndex < startEventIndex
        val lo = minOf(startEventIndex, endEventIndex)
        val hi = maxOf(startEventIndex, endEventIndex)

        if (lo == hi) return PlaybackExtraction(emptyList(), isBackward)

        val fromExclusive = (lo + 1).coerceAtLeast(0)
        val toInclusive = hi.coerceAtMost(events.lastIndex)

        val intents = events.subList(fromExclusive, toInclusive + 1)
            .filter { it.type == StoreEventType.INTENT }
            .map { event ->
                StoreIntent(
                    storeName = event.storeName,
                    intentValue = event.value,
                    timestamp = event.id
                )
            }

        return PlaybackExtraction(intents, isBackward)
    }

    /**
     * 回溯模式：从 controller 事件列表截取 events[0..upToIndex] 构建 TTE。
     * 绕过 exportAllStores()（它读 RestoreRegistry，回溯步进不更新 Registry）。
     * unusedStoreStates 留空：TteStateExtractor.extract() 只读 recordedEvents。
     */
    /**
     * 回溯模式：从 controller 事件列表截取 events[0..upToIndex] 构建 TTE。
     * position -1 返回空字节——验证脚本遇到空 start.tte 会 force-stop 回到初始状态。
     */
    private fun exportPlaybackTte(upToEventIndex: Int): ByteArray {
        if (upToEventIndex < 0) return ByteArray(0)

        val events = timeTravelController.state.events
        // ArrayList 副本：SubList 不可 Java Serializable
        val sliced = ArrayList(events.subList(0, (upToEventIndex + 1).coerceAtMost(events.size)))
        val export = TimeTravelExport(
            recordedEvents = sliced,
            unusedStoreStates = emptyMap()
        )
        return when (val result = serializeTimeTravelExport(export)) {
            is ExportResult.Success -> result.data
            is ExportResult.Error -> {
                com.example.archshowcase.core.util.Log.e("VF") { "exportPlaybackTte failed: ${result.message}" }
                ByteArray(0)
            }
        }
    }

    override fun onImport(data: ByteArray): ImportResult =
        when (val result = deserializeTimeTravelExport(data)) {
            is TimeTravelExportSerializer.Result.Success<*> -> {
                val export = result.data as com.arkivanov.mvikotlin.timetravel.export.TimeTravelExport
                RestoreRegistry.restoreFromEvents(export.recordedEvents)
                timeTravelController.import(export)
                ImportResult.Success(emptyList())
            }
            is TimeTravelExportSerializer.Result.Error -> {
                ImportResult.Error("导入失败: ${result.exception.message}")
            }
        }

    override fun onMoveToStart() {
        timeTravelController.moveToStart()
    }

    override fun onStepBackward() {
        timeTravelController.stepBackward()
    }

    override fun onStepForward() {
        timeTravelController.stepForward()
    }

    override fun onMoveToEnd() {
        timeTravelController.moveToEnd()
    }

    override fun onCancel() {
        timeTravelController.cancel()
        navigator.restore()
    }

    companion object {
        internal const val MSG_BOOKMARK_SAVED = "书签已保存，重启后自动恢复"
        internal const val MSG_BOOKMARK_CLEARED = "书签已清除"
        internal const val MSG_BOOKMARK_SAVE_FAILED = "书签保存失败"
    }
}
