package com.example.archshowcase.core.perf.jank

import android.os.Debug
import android.os.SystemClock
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.perf.gc.GcPressureDetector
import com.example.archshowcase.core.perf.platform.FrameDiagnostics

/**
 * 帧间 GC + CPU 调度诊断采集。
 *
 * GC 统计来源（ART runtime stats）：
 * - `art.gc.gc-count/time`：所有 GC（含并发），时间是 GC 线程耗时
 * - `art.gc.blocking-gc-count/time`：阻塞式 GC，直接暂停主线程
 */
object FrameDiagnosticsCollector {

    private var lastGcCount: Long = 0
    private var lastGcTimeMs: Long = 0
    private var lastBlockingGcCount: Long = 0
    private var lastBlockingGcTimeMs: Long = 0
    private var lastCpuTimeMs: Long = 0
    private var lastWallTimeMs: Long = 0

    private var current = FrameDiagnostics()

    fun install() {
        lastGcCount = stat("art.gc.gc-count")
        lastGcTimeMs = stat("art.gc.gc-time")
        lastBlockingGcCount = stat("art.gc.blocking-gc-count")
        lastBlockingGcTimeMs = stat("art.gc.blocking-gc-time")
        lastCpuTimeMs = SystemClock.currentThreadTimeMillis()
        lastWallTimeMs = SystemClock.elapsedRealtime()
    }

    fun uninstall() {
        current = FrameDiagnostics()
    }

    fun onFrameBoundary() {
        val nowGcCount = stat("art.gc.gc-count")
        val nowGcTimeMs = stat("art.gc.gc-time")
        val nowBlockingGcCount = stat("art.gc.blocking-gc-count")
        val nowBlockingGcTimeMs = stat("art.gc.blocking-gc-time")
        val nowCpuTimeMs = SystemClock.currentThreadTimeMillis()
        val nowWallTimeMs = SystemClock.elapsedRealtime()

        current = FrameDiagnostics(
            gcCount = (nowGcCount - lastGcCount).toInt(),
            gcTimeMs = nowGcTimeMs - lastGcTimeMs,
            blockingGcCount = (nowBlockingGcCount - lastBlockingGcCount).toInt(),
            blockingGcTimeMs = nowBlockingGcTimeMs - lastBlockingGcTimeMs,
            cpuTimeMs = nowCpuTimeMs - lastCpuTimeMs,
            wallTimeMs = nowWallTimeMs - lastWallTimeMs
        )

        lastGcCount = nowGcCount
        lastGcTimeMs = nowGcTimeMs
        lastBlockingGcCount = nowBlockingGcCount
        lastBlockingGcTimeMs = nowBlockingGcTimeMs
        lastCpuTimeMs = nowCpuTimeMs
        lastWallTimeMs = nowWallTimeMs

        // 喂给 GC 压力检测器
        if (current.blockingGcCount > 0 && AppConfig.enableGcPressureDetection) {
            GcPressureDetector.onFrame(current.blockingGcCount, current.blockingGcTimeMs)
        }
    }

    fun snapshot(): FrameDiagnostics = current

    private fun stat(key: String): Long =
        Debug.getRuntimeStat(key).toLongOrNull() ?: 0
}
