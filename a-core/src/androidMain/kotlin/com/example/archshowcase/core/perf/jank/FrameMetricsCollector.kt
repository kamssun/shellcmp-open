package com.example.archshowcase.core.perf.jank

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.util.Log

private const val TAG = "PERF"

/**
 * 利用 Window.OnFrameMetricsAvailableListener 采集每帧的耗时分解。
 *
 * FrameMetrics 在 GPU 完成后异步到达 bg thread，晚于主线程卡顿检测。
 * 因此采用反向匹配：主线程记录待匹配的 vsyncNs，FrameMetrics 到达时主动输出。
 */
object FrameMetricsCollector {

    data class Breakdown(
        val vsyncNs: Long = 0L,
        val delayMs: Float = 0f,
        val inputMs: Float = 0f,
        val animationMs: Float = 0f,
        val layoutMs: Float = 0f,
        val drawMs: Float = 0f,
        val syncMs: Float = 0f,
        val gpuMs: Float = 0f,
        val totalMs: Float = 0f
    )

    private var bgThread: HandlerThread? = null

    /** 当前帧的 vsync timestamp（由 AndroidFrameMonitor.doFrame 写入） */
    @Volatile
    var currentVsyncNs: Long = 0L

    /**
     * 待匹配的卡顿帧 vsyncNs（主线程写，bg thread 读）。
     * 主线程检测到卡顿后设置，FrameMetrics 到达时匹配并输出分解。
     */
    @Volatile
    private var pendingVsyncNs: Long = 0L

    @Volatile
    private var pendingFrameIndex: Long = 0L

    @Volatile
    private var pendingSeverity: JankSeverity = JankSeverity.SLIGHT

    /** 主线程调用：标记卡顿帧等待 FrameMetrics 分解 */
    fun markPendingJank(vsyncNs: Long, frameIndex: Long, severity: JankSeverity) {
        pendingVsyncNs = vsyncNs
        pendingFrameIndex = frameIndex
        pendingSeverity = severity
    }

    private val listener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
        val copy = FrameMetrics(frameMetrics)
        val vsyncNs = if (android.os.Build.VERSION.SDK_INT >= 26) copy.getMetric(FrameMetrics.VSYNC_TIMESTAMP) else 0L

        // 检查是否匹配待输出的卡顿帧
        val pending = pendingVsyncNs
        if (pending > 0 && vsyncNs == pending) {
            pendingVsyncNs = 0L
            val severity = pendingSeverity
            val phases = mutableListOf<String>()
            val delayMs = copy.ns(FrameMetrics.UNKNOWN_DELAY_DURATION)
            val inputMs = copy.ns(FrameMetrics.INPUT_HANDLING_DURATION)
            val animationMs = copy.ns(FrameMetrics.ANIMATION_DURATION)
            val layoutMs = copy.ns(FrameMetrics.LAYOUT_MEASURE_DURATION)
            val drawMs = copy.ns(FrameMetrics.DRAW_DURATION)
            val syncMs = copy.ns(FrameMetrics.SYNC_DURATION)
            val gpuMs = if (android.os.Build.VERSION.SDK_INT >= 31) copy.ns(FrameMetrics.GPU_DURATION) else 0f
            val totalMs = copy.ns(FrameMetrics.TOTAL_DURATION)
            if (delayMs >= 1f) phases.add("delay ${delayMs.toInt()}ms")
            if (inputMs >= 1f) phases.add("input ${inputMs.toInt()}ms")
            if (animationMs >= 1f) phases.add("anim(recompose→apply) ${animationMs.toInt()}ms")
            if (layoutMs >= 1f) phases.add("layout ${layoutMs.toInt()}ms")
            if (drawMs >= 1f) phases.add("draw(m&l→canvas) ${drawMs.toInt()}ms")
            if (syncMs >= 1f) phases.add("sync ${syncMs.toInt()}ms")
            if (gpuMs >= 1f) phases.add("gpu ${gpuMs.toInt()}ms")
            if (phases.isNotEmpty()) {
                val msg = { "[PERF:JANK] #${pendingFrameIndex} Choreographer.doFrame拆解: ${phases.joinToString(" | ")} (total ${totalMs.toInt()}ms)" }
                when (severity) {
                    JankSeverity.SLIGHT -> Log.d(TAG, msg)
                    JankSeverity.MODERATE -> Log.w(TAG, msg)
                    JankSeverity.SEVERE, JankSeverity.FROZEN -> Log.e(TAG, message = msg)
                }
            }
        }
    }

    fun install(activity: ComponentActivity) {
        val thread = HandlerThread("FrameMetrics").apply { start() }
        bgThread = thread
        activity.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                activity.window.removeOnFrameMetricsAvailableListener(listener)
                thread.quitSafely()
                bgThread = null
                currentVsyncNs = 0L
                pendingVsyncNs = 0L
            }
        })
    }

    private fun FrameMetrics.ns(id: Int): Float =
        getMetric(id) / 1_000_000f
}
