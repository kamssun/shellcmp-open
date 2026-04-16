package com.example.archshowcase.core.perf.page

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.FrameTiming
import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.TransitionInfo
import com.example.archshowcase.core.perf.platform.currentUptimeMs

class PageTracker(
    private val deviceInfoProvider: () -> DeviceInfo,
    private val onPageMetrics: (PageFrameMetrics) -> Unit
) {
    var currentRoute: String = ""
        private set

    var currentTransition: TransitionInfo? = null
        private set

    private var pageStartMs: Long = 0L
    private var transitionDurationMs: Long? = null
    private val accumulator = FrameAccumulator()

    fun start(initialRoute: String) {
        currentRoute = initialRoute
        pageStartMs = currentUptimeMs()
        accumulator.reset()
    }

    fun onTransitionStart(from: String, to: String) {
        flushCurrentPage()
        currentTransition = TransitionInfo(
            fromRoute = from,
            toRoute = to,
            startedAtMs = currentUptimeMs()
        )
    }

    fun onTransitionEnd(route: String) {
        val transition = currentTransition
        transitionDurationMs = if (transition != null) {
            currentUptimeMs() - transition.startedAtMs
        } else {
            null
        }
        currentTransition = null
        currentRoute = route
        pageStartMs = currentUptimeMs()
        accumulator.reset()
    }

    fun onFrame(timing: FrameTiming) {
        accumulator.addFrame(timing)
    }

    fun recordJank(event: JankEvent) {
        accumulator.addJank(event)
    }

    fun onPagePaused(): PageFrameMetrics? {
        if (currentRoute.isEmpty()) return null
        val metrics = buildMetrics()
        return if (metrics.totalFrames > 0) metrics else null
    }

    private fun flushCurrentPage() {
        if (currentRoute.isEmpty()) return
        val metrics = buildMetrics()
        if (metrics.totalFrames > 0) {
            onPageMetrics(metrics)
        }
        accumulator.reset()
    }

    private fun buildMetrics(): PageFrameMetrics {
        val now = currentUptimeMs()
        val durationMs = now - pageStartMs
        val avgFps = if (durationMs > 0) {
            accumulator.totalFrames.toFloat() / (durationMs / 1000f)
        } else {
            0f
        }
        return PageFrameMetrics(
            route = currentRoute,
            durationMs = durationMs,
            totalFrames = accumulator.totalFrames,
            droppedFrames = accumulator.droppedFrames,
            avgFps = avgFps,
            jankCounts = accumulator.jankCounts.toMap(),
            worstJank = accumulator.worstJank,
            transitionDurationMs = transitionDurationMs,
            deviceInfo = deviceInfoProvider()
        )
    }

    internal fun reset() {
        currentRoute = ""
        currentTransition = null
        pageStartMs = 0L
        transitionDurationMs = null
        accumulator.reset()
    }
}

internal class FrameAccumulator {
    var totalFrames: Int = 0
        private set
    var droppedFrames: Int = 0
        private set
    val jankCounts = mutableMapOf<JankSeverity, Int>()
    var worstJank: JankEvent? = null
        private set

    fun addFrame(timing: FrameTiming) {
        totalFrames++
        val dropped = timing.droppedFrames
        if (dropped > 0) droppedFrames += dropped
    }

    fun addJank(event: JankEvent) {
        jankCounts[event.severity] = (jankCounts[event.severity] ?: 0) + 1
        val current = worstJank
        if (current == null || event.droppedFrames > current.droppedFrames) {
            worstJank = event
        }
    }

    fun reset() {
        totalFrames = 0
        droppedFrames = 0
        jankCounts.clear()
        worstJank = null
    }
}
