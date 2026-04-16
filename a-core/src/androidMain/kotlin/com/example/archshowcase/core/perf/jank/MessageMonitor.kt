package com.example.archshowcase.core.perf.jank

import android.os.Looper
import android.os.SystemClock
import android.util.Printer
import com.example.archshowcase.core.scheduler.OBODiagnostics
import com.example.archshowcase.core.scheduler.OBOScheduler

/**
 * 利用 Looper.setMessageLogging 追踪主线程帧间所有慢 Message。
 *
 * 时间线（帧边界 = Choreographer `>>>>> Dispatching`）：
 * ```
 * Frame A:
 *   >>>>> Choreographer (帧边界: swap)
 *     doFrame(A) → 读 slowMessages = 上一帧间的慢消息
 *     render...
 *   <<<<< Finished → 记录 "Choreographer 30ms" 到 frameMessages
 *
 *   [msg M1 5ms] [msg M2 12ms] → 记录到 frameMessages
 *
 * Frame B:
 *   >>>>> Choreographer (帧边界: swap → slowMessages = [Choreographer 30ms, M1 5ms, M2 12ms])
 *     doFrame(B) → 检测到卡顿 → 读 slowMessages ← 完整包含导致卡顿的所有慢消息
 * ```
 */
object MessageMonitor : Printer {

    private var dispatchStartMs: Long = 0L
    private var dispatchMsg: String? = null
    private var frameCounter: Long = 0L

    /** 当前帧间累积的慢 Message */
    private val frameMessages = mutableListOf<String>()

    /** 上一帧间的慢 Message（供卡顿报告读取） */
    private var lastFrameMessages: List<String> = emptyList()

    /**
     * 上一帧的 vsync timestamp（ns）。
     * 在帧边界 swap 时从 FrameMetricsCollector.currentVsyncNs 快照，
     * 此时 FrameCallback 尚未执行，currentVsyncNs 仍是上一帧的值。
     */
    var lastFrameVsyncNs: Long = 0L
        private set

    /** 当前帧序号（从 1 递增），用于对齐 btrace */
    val frameIndex: Long get() = frameCounter

    fun install() {
        FrameDiagnosticsCollector.install()
        Looper.getMainLooper().setMessageLogging(this)
    }

    fun uninstall() {
        Looper.getMainLooper().setMessageLogging(null)
        FrameDiagnosticsCollector.uninstall()
        frameMessages.clear()
        lastFrameMessages = emptyList()
        dispatchMsg = null
        frameCounter = 0L
        lastFrameVsyncNs = 0L
    }

    /** 上一帧间隔内的慢 Message 列表（含帧渲染自身） */
    val slowMessages: List<String> get() = lastFrameMessages

    override fun println(x: String?) {
        if (x == null) return
        if (x.startsWith(">>>>> Dispatching")) {
            // 每条新消息清理残留 OBO tag，防止非 OBO 消息继承上一个 OBO 的 tag
            OBOScheduler.currentTaskTag = null
            // Choreographer 帧回调开始 = 帧边界，交换缓冲区 + 诊断采样
            if (x.contains("Choreographer\$FrameHandler")) {
                // 帧边界：短任务汇总补位（仅当本帧没有 OBO 逐条慢消息时注入）
                val hasOboSlow = frameMessages.any { it.startsWith("OBOScheduler ") }
                OBODiagnostics.frameSummaryIfNeeded(hasOboSlow)?.let { frameMessages.add(it) }
                OBODiagnostics.resetFrameStats()
                lastFrameMessages = if (frameMessages.isEmpty()) emptyList() else frameMessages.toList()
                frameMessages.clear()
                frameCounter++
                // 快照上一帧的 vsyncNs（此时 FrameCallback 尚未执行，值仍是上一帧的）
                lastFrameVsyncNs = FrameMetricsCollector.currentVsyncNs
                FrameDiagnosticsCollector.onFrameBoundary()
            }
            dispatchStartMs = SystemClock.elapsedRealtime()
            dispatchMsg = x
        } else if (x.startsWith("<<<<< Finished")) {
            val durationMs = SystemClock.elapsedRealtime() - dispatchStartMs
            if (durationMs >= SLOW_THRESHOLD_MS && dispatchMsg != null) {
                if (frameMessages.size < MAX_MESSAGES) {
                    val oboTag = OBOScheduler.currentTaskTag
                    val label = if (oboTag != null) {
                        "OBOScheduler $oboTag"
                    } else {
                        parseHandler(dispatchMsg!!)
                    }
                    frameMessages.add("$label ${durationMs}ms")
                }
            }
            dispatchMsg = null
        }
    }

    private fun parseHandler(msg: String): String {
        // 输入: ">>>>> Dispatching to Handler (android.view.Choreographer$FrameHandler) {8b1ba6b} android.view.Choreographer$FrameDisplayEventReceiver@c29c486: 0"
        // 输出: "Choreographer" 或 "OBOScheduler"
        val body = msg.removePrefix(">>>>> Dispatching to Handler (")
        val classEnd = body.indexOf(')')
        if (classEnd < 0) return body.take(60)
        val fullClass = body.substring(0, classEnd)
        val shortClass = fullClass.substringAfterLast('.')

        // 提取 callback 部分（去掉 {hash} 和 @hash: what）
        val hashEnd = body.indexOf('}', classEnd)
        val afterHash = if (hashEnd >= 0) body.substring(hashEnd + 1).trim() else ""
        val callbackRaw = afterHash.substringBefore('@').substringAfterLast('.').trim()
        // 去掉编译器生成的 lambda 后缀 ($ExternalSyntheticLambda0 等)
        val callback = callbackRaw.substringBefore("\$ExternalSynthetic")
            .substringBefore("\$\$Lambda")
            .ifEmpty { null }

        // handler 和 callback 同类前缀则省略 callback（如 Choreographer）
        val handlerPrefix = shortClass.substringBefore('$')
        return when {
            callback == null -> handlerPrefix
            callback.startsWith(handlerPrefix) -> handlerPrefix
            shortClass == "Handler" -> callback  // 通用 Handler，直接用 callback
            else -> "$handlerPrefix/$callback"
        }
    }

    /** 慢于此阈值的 Message 才记录（ms） */
    private const val SLOW_THRESHOLD_MS = 3L

    /** 单帧最多记录条数，防止极端情况内存膨胀 */
    private const val MAX_MESSAGES = 20
}
