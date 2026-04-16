package com.example.archshowcase.core.perf.platform

import android.view.Choreographer
import com.example.archshowcase.core.perf.jank.FrameMetricsCollector
import com.example.archshowcase.core.perf.jank.MessageMonitor
import com.example.archshowcase.core.perf.model.FrameTiming
import com.example.archshowcase.core.scheduler.OBODiagnostics

actual class FrameMonitor actual constructor() {

    private var callback: ((FrameTiming) -> Unit)? = null
    private var lastFrameTimeNanos: Long = 0L
    private var running = false
    private var cachedExpectedMs: Float = 1000f / 60f

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            // 顺便写入 vsyncNs 供 FrameMetrics 匹配，省掉 FrameMetricsCollector 自己的 FrameCallback
            FrameMetricsCollector.currentVsyncNs = frameTimeNanos
            if (lastFrameTimeNanos > 0) {
                val durationMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                // 锁屏/后台恢复后首帧 delta 可能极大，跳过避免误报 FROZEN
                if (durationMs > MAX_GAP_MS) {
                    lastFrameTimeNanos = frameTimeNanos
                    Choreographer.getInstance().postFrameCallback(this)
                    return
                }
                callback?.invoke(
                    FrameTiming(
                        frameStartMs = frameTimeNanos / 1_000_000,
                        frameDurationMs = durationMs,
                        expectedDurationMs = cachedExpectedMs
                    )
                )
            }
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    actual fun start(callback: (FrameTiming) -> Unit) {
        this.callback = callback
        running = true
        lastFrameTimeNanos = 0L
        val refreshRate = deviceRefreshRate()
        cachedExpectedMs = 1000f / refreshRate
        OBODiagnostics.init(refreshRate)
        MessageMonitor.install()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    actual fun stop() {
        running = false
        callback = null
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        MessageMonitor.uninstall()
    }

    companion object {
        /** 超过 2 秒的帧间隔视为非正常渲染间隙（锁屏/后台），直接跳过 */
        private const val MAX_GAP_MS = 2000f
    }
}
