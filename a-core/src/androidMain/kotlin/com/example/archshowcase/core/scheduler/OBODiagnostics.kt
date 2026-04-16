package com.example.archshowcase.core.scheduler

import android.os.SystemClock
import com.example.archshowcase.core.util.Log

private const val TAG_SLOW = "OBO:SLOW"
private const val TAG_FLOOD = "OBO:FLOOD"

/**
 * OBO 调度器诊断：精确定位哪个来源导致了主线程卡顿。
 *
 * 三种检测：
 * 1. 慢任务 — 单个任务执行超过半帧（阈值跟刷新率挂钩）
 * 2. 累计耗时 — 1s 窗口内某来源累计执行超过帧预算的 10%
 * 3. 队列深度 — 待执行任务数超过水位线
 *
 * 线程安全：所有方法仅在主线程调用（OBO 任务只在主线程执行）。
 */
object OBODiagnostics {

    private var halfFrameMs: Float = 8.3f
    private var floodThresholdMs: Float = 100f
    private const val DEPTH_HIGH_WATER_MARK = 512
    private var lastDepthAlertMs: Long = 0L

    /** 累计耗时滑动窗口（1s） */
    private val windows = HashMap<String, FloodWindow>(16)

    /** 当前帧内各来源执行耗时 */
    private val frameStats = HashMap<String, Float>(16)

    private class FloodWindow {
        var totalMs: Float = 0f
        var count: Int = 0
        var windowStartMs: Long = 0L
        var lastAlertMs: Long = 0L
    }

    fun init(refreshRate: Float) {
        val rate = if (refreshRate > 0f) refreshRate else 60f
        halfFrameMs = 1000f / rate / 2f
        // 帧预算/秒 = 1000ms（与刷新率无关），10% = 100ms
        floodThresholdMs = 100f
    }

    /**
     * 记录一次任务执行。
     *
     * @param tag 来源标识
     * @param elapsedNs 执行耗时（纳秒）
     * @param queueDepth 当前队列剩余任务数
     */
    fun record(tag: String, elapsedNs: Long, queueDepth: Int = 0) {
        val elapsedMs = elapsedNs / 1_000_000f

        // 帧内统计
        frameStats[tag] = (frameStats[tag] ?: 0f) + elapsedMs

        // 慢任务检测
        if (elapsedMs > halfFrameMs) {
            Log.w(TAG_SLOW) {
                "$tag ${formatMs(elapsedMs)} (threshold=${formatMs(halfFrameMs)})"
            }
        }

        // 累计耗时窗口
        val now = SystemClock.elapsedRealtime()
        val window = windows.getOrPut(tag) {
            FloodWindow().also { it.windowStartMs = now }
        }

        if (now - window.windowStartMs > WINDOW_MS) {
            window.totalMs = 0f
            window.count = 0
            window.windowStartMs = now
        }

        window.totalMs += elapsedMs
        window.count++

        if (window.totalMs > floodThresholdMs && now - window.lastAlertMs >= COOLDOWN_MS) {
            window.lastAlertMs = now
            Log.w(TAG_FLOOD) {
                "$tag ${formatMs(window.totalMs)}/${WINDOW_MS}ms " +
                    "(threshold=${formatMs(floodThresholdMs)}, count=${window.count}, cooldown=${COOLDOWN_MS / 1000}s)"
            }
        }

        // 队列深度检测
        if (queueDepth > DEPTH_HIGH_WATER_MARK && now - lastDepthAlertMs >= COOLDOWN_MS) {
            lastDepthAlertMs = now
            Log.w(TAG_FLOOD) {
                "$tag depth=$queueDepth (high_water=$DEPTH_HIGH_WATER_MARK)"
            }
        }
    }

    /** 帧边界清空帧内统计 */
    fun resetFrameStats() {
        frameStats.clear()
    }

    /**
     * 帧内 OBO 来源汇总（按耗时降序），用于短任务场景补位 JANK 报告。
     *
     * @param hasOboSlowMessages 本帧是否已有 OBO 逐条慢消息
     * @return 汇总字符串，无数据或已有逐条记录时返回 null
     */
    fun frameSummaryIfNeeded(hasOboSlowMessages: Boolean): String? {
        if (hasOboSlowMessages || frameStats.isEmpty()) return null
        val totalMs = frameStats.values.sum()
        if (totalMs < 1f) return null
        val sources = frameStats.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { "${it.key} ${formatMs(it.value)}" }
        return "OBO ${formatMs(totalMs)} [$sources]"
    }

    private fun formatMs(ms: Float): String = "%.1fms".format(ms)

    private const val WINDOW_MS = 1000L
    private const val COOLDOWN_MS = 5000L
}
