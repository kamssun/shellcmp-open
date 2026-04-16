package com.example.archshowcase.core.perf.jank

import com.example.archshowcase.core.perf.PerfConfig
import com.example.archshowcase.core.perf.model.FrameInfo
import com.example.archshowcase.core.perf.model.JankContext
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.perf.model.MemoryInfo
import com.example.archshowcase.core.perf.model.StateDigest
import com.example.archshowcase.core.perf.page.PageTracker
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.scroll.ScrollRestorableState
import com.example.archshowcase.core.perf.platform.currentFrameIndex
import com.example.archshowcase.core.perf.platform.currentLooperMessage
import com.example.archshowcase.core.perf.platform.currentVsyncNs
import com.example.archshowcase.core.perf.platform.markPendingJank
import com.example.archshowcase.core.perf.platform.snapshotFrameDiagnostics
import com.example.archshowcase.core.trace.user.IntentRecord
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ContextCollector(
    private val config: PerfConfig,
    private val intentProvider: () -> List<IntentRecord> = { emptyList() },
    private val snapshotProvider: () -> Map<String, RestorableState> = { emptyMap() },
    private val memoryProvider: () -> MemoryInfo = { MemoryInfo(0, 0, 0f) }
) {

    fun collect(
        severity: JankSeverity,
        pageTracker: PageTracker,
        frameInfo: FrameInfo
    ): JankContext {
        val intents = if (config.contextCollectionEnabled) {
            intentProvider()
        } else {
            emptyList()
        }

        val snapshots = if (config.contextCollectionEnabled) {
            snapshotProvider()
        } else {
            emptyMap()
        }

        val digests = snapshots.map { (name, state) -> name to toDigest(name, state) }.toMap()

        val fullStates = if (
            config.fullStateOnSevereJank &&
            (severity == JankSeverity.SEVERE || severity == JankSeverity.FROZEN)
        ) {
            snapshots.mapValues { (name, state) -> stateToJson(name, state).toString() }
        } else {
            null
        }

        val frameIndex = currentFrameIndex()

        // 标记待匹配：FrameMetrics 异步到达后自动输出分解
        markPendingJank(currentVsyncNs(), frameIndex, severity)

        return JankContext(
            recentIntents = intents,
            stateSnapshots = digests,
            fullStates = fullStates,
            activeRoute = pageTracker.currentRoute,
            transitionInfo = pageTracker.currentTransition,
            frameInfo = frameInfo,
            memoryInfo = memoryProvider(),
            message = currentLooperMessage(),
            diagnostics = snapshotFrameDiagnostics(),
            frameIndex = frameIndex
        )
    }

    companion object {
        fun toDigest(name: String, state: RestorableState): StateDigest {
            val historySize = (state as? ReplayableState<*>)?.history?.size
            val scrollPos = (state as? ScrollRestorableState)?.scrollPosition
            return StateDigest(
                storeName = name,
                stateClassName = state::class.simpleName ?: "Unknown",
                hasValidData = state.hasValidData(),
                historySize = historySize,
                scrollPosition = scrollPos,
                stateHashCode = state.hashCode()
            )
        }

        /**
         * State 未标 @Serializable，用结构化 JSON 输出 digest 字段。
         * 不调用 state.toString()，避免在主线程阻塞 10-100ms。
         */
        private fun stateToJson(name: String, state: RestorableState): JsonObject {
            val digest = toDigest(name, state)
            return buildJsonObject {
                put("store", digest.storeName)
                put("class", digest.stateClassName)
                put("hasValidData", digest.hasValidData)
                digest.historySize?.let { put("historySize", it) }
                digest.scrollPosition?.let {
                    put("scrollIndex", it.firstVisibleIndex)
                    put("scrollOffset", it.offset)
                }
                put("hashCode", digest.stateHashCode)
            }
        }
    }
}
