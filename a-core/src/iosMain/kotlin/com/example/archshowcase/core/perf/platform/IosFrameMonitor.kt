package com.example.archshowcase.core.perf.platform

import com.example.archshowcase.core.perf.model.FrameTiming
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject
import platform.objc.sel_registerName

@OptIn(ExperimentalForeignApi::class)
actual class FrameMonitor actual constructor() {

    private var callback: ((FrameTiming) -> Unit)? = null
    private var displayLink: CADisplayLink? = null
    private var lastTimestamp: Double = 0.0
    private var target: DisplayLinkTarget? = null
    private var cachedExpectedMs: Float = 1000f / 60f

    actual fun start(callback: (FrameTiming) -> Unit) {
        this.callback = callback
        lastTimestamp = 0.0
        cachedExpectedMs = 1000f / deviceRefreshRate()

        val linkTarget = DisplayLinkTarget { link ->
            val timestamp = link.timestamp
            if (lastTimestamp > 0.0) {
                val durationMs = ((timestamp - lastTimestamp) * 1000).toFloat()
                // 后台恢复后首帧 delta 可能极大，跳过避免误报 FROZEN
                if (durationMs > MAX_GAP_MS) {
                    lastTimestamp = timestamp
                    return@DisplayLinkTarget
                }
                this.callback?.invoke(
                    FrameTiming(
                        frameStartMs = (timestamp * 1000).toLong(),
                        frameDurationMs = durationMs,
                        expectedDurationMs = cachedExpectedMs
                    )
                )
            }
            lastTimestamp = timestamp
        }
        target = linkTarget

        displayLink = CADisplayLink.displayLinkWithTarget(
            target = linkTarget,
            selector = sel_registerName("tick:")!!
        ).apply {
            addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
        }
    }

    actual fun stop() {
        displayLink?.invalidate()
        displayLink = null
        callback = null
        target = null
        lastTimestamp = 0.0
    }

    companion object {
        private const val MAX_GAP_MS = 2000f
    }
}

@Suppress("UNUSED_PARAMETER")
private class DisplayLinkTarget(
    private val onTick: (CADisplayLink) -> Unit
) : NSObject() {

    @kotlinx.cinterop.ObjCAction
    fun tick(link: CADisplayLink) {
        onTick(link)
    }
}
