package com.example.archshowcase.core.perf.jank

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.FrameInfo
import com.example.archshowcase.core.perf.model.FrameTiming
import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.perf.page.PageTracker
import com.example.archshowcase.core.perf.platform.currentUptimeMs
import com.example.archshowcase.core.perf.startup.StartupTracer

class JankTracker(
    private val contextCollector: ContextCollector,
    private val pageTracker: PageTracker,
    private val deviceInfoProvider: () -> DeviceInfo,
    private val onJank: (JankEvent) -> Unit
) {
    private var warmupFrames = WARMUP_FRAME_COUNT

    fun onFrame(timing: FrameTiming) {
        // 启动阶段 + 启动后前几帧不检测卡顿（首帧必然超长）
        if (StartupTracer.isActive) {
            warmupFrames = WARMUP_FRAME_COUNT
            return
        }
        if (warmupFrames > 0) {
            warmupFrames--
            return
        }
        val dropped = timing.droppedFrames
        val severity = JankSeverity.fromDroppedFrames(dropped) ?: return

        val frameInfo = FrameInfo(
            droppedFrames = dropped,
            durationMs = timing.frameDurationMs,
            refreshRate = 1000f / timing.expectedDurationMs
        )

        val context = contextCollector.collect(
            severity = severity,
            pageTracker = pageTracker,
            frameInfo = frameInfo
        )

        val event = JankEvent(
            severity = severity,
            droppedFrames = dropped,
            durationMs = timing.frameDurationMs,
            expectedFrameMs = timing.expectedDurationMs,
            context = context,
            deviceInfo = deviceInfoProvider(),
            timestamp = currentUptimeMs()
        )

        onJank(event)
    }

    companion object {
        private const val WARMUP_FRAME_COUNT = 3
    }
}
