package com.example.archshowcase.core.perf.gc

import android.os.Debug
import android.os.SystemClock
import com.example.archshowcase.core.perf.model.MemoryInfo
import com.example.archshowcase.core.perf.platform.memoryInfo
import com.example.archshowcase.core.util.Log

private const val TAG = "PERF:GC_PRESSURE"

/**
 * GC 压力检测器。
 *
 * 在每帧边界接收阻塞 GC 增量，维护 5s 翻滚窗口计算 pressure 评分，
 * 超阈值时输出内存分类摘要（L1）和 Store 集合 size 快照（L2）。
 *
 * pressure = blockingGcTimeMs/s × (1 - freePercent/100)
 *
 * 基于阻塞时长而非次数：1 次 200ms 的 GC 比 10 次 1ms 的影响大得多。
 *
 * 线程安全：所有方法仅在主线程调用（与 FrameDiagnosticsCollector 相同）。
 */
object GcPressureDetector {

    private val storeSnapshotProvider get() = GcPressureSetup.storeSnapshotProvider

    // ── 翻滚窗口 ──────────────────────────────────
    private const val WINDOW_MS = 5000L
    private const val MIN_EVAL_MS = 1000L
    private const val COOLDOWN_MS = 10_000L

    private var windowStartMs: Long = 0L
    private var windowBlockingCount: Int = 0
    private var windowBlockingTimeMs: Long = 0L
    private var lastAlertMs: Long = 0L
    private var lastAlertLevel: GcPressureLevel? = null

    // ── 阈值（基于 blocking ms/s × 内存因子） ──────
    // 20 ≈ 20ms/s blocking at 100% memory, or 40ms/s at 50%
    private const val PRESSURE_WARN = 20f
    // 60 ≈ 60ms/s blocking at 100% memory — 每秒 6% 主线程暂停
    private const val PRESSURE_SEVERE = 60f
    // 150 ≈ 每秒 15%+ 主线程暂停，接近不可用
    private const val PRESSURE_CRITICAL = 150f
    private const val FREE_PERCENT_CRITICAL = 10f

    /**
     * 每帧调用，喂入本帧的阻塞 GC 增量。
     *
     * @param blockingGcCount 本帧新增阻塞 GC 次数
     * @param blockingGcTimeMs 本帧新增阻塞 GC 暂停时长
     */
    fun onFrame(blockingGcCount: Int, blockingGcTimeMs: Long) {
        if (blockingGcCount <= 0) return

        val now = SystemClock.elapsedRealtime()

        // 窗口过期则重置
        if (now - windowStartMs > WINDOW_MS) {
            windowBlockingCount = 0
            windowBlockingTimeMs = 0L
            windowStartMs = now
        }

        windowBlockingCount += blockingGcCount
        windowBlockingTimeMs += blockingGcTimeMs

        // 窗口不足 1s 不评估，避免除数过小导致 pressure 虚高
        val elapsed = now - windowStartMs
        if (elapsed < MIN_EVAL_MS) return

        val mem = memoryInfo()
        val elapsedSec = elapsed.toFloat() / 1000f
        val blockingMsPerSec = windowBlockingTimeMs.toFloat() / elapsedSec
        val memoryFactor = 1f - mem.freePercent / 100f
        val pressure = blockingMsPerSec * memoryFactor

        val level = when {
            pressure >= PRESSURE_CRITICAL || mem.freePercent < FREE_PERCENT_CRITICAL -> GcPressureLevel.CRITICAL
            pressure >= PRESSURE_SEVERE -> GcPressureLevel.SEVERE
            pressure >= PRESSURE_WARN -> GcPressureLevel.WARN
            else -> return
        }

        // cooldown 防刷屏，但级别升级时突破 cooldown
        val upgrading = lastAlertLevel?.let { level > it } == true
        if (!upgrading && now - lastAlertMs < COOLDOWN_MS) return
        lastAlertMs = now
        lastAlertLevel = level

        report(level, pressure, mem)
    }

    fun clear() {
        windowStartMs = 0L
        windowBlockingCount = 0
        windowBlockingTimeMs = 0L
        lastAlertMs = 0L
        lastAlertLevel = null
    }

    // ── 报告 ───────────────────────────────────────

    private fun report(level: GcPressureLevel, pressure: Float, mem: MemoryInfo) {
        val sb = StringBuilder()

        // 第一行：级别 + pressure + GC 概况 + heap
        sb.append("[GC_PRESSURE] ${level.name}  pressure=%.1f".format(pressure))
        sb.append(" | ${windowBlockingCount} GC ${windowBlockingTimeMs}ms / ${WINDOW_MS / 1000}s")
        sb.append(" | heap ${mem.usedMb}/${mem.totalMb}MB (${mem.freePercent.toInt()}% free)")

        // PSS 分类 + Store 快照仅 SEVERE/CRITICAL 输出（WARN 避免 Debug.getMemoryInfo 开销）
        if (level >= GcPressureLevel.SEVERE) {
            appendPssSummary(sb)
            appendStoreSnapshots(sb)
        }

        val message = sb.toString()
        when (level) {
            GcPressureLevel.WARN -> Log.w(TAG) { message }
            GcPressureLevel.SEVERE -> Log.e(TAG) { message }
            GcPressureLevel.CRITICAL -> Log.wtf(TAG) { message }
        }
    }

    private fun appendPssSummary(sb: StringBuilder) {
        val mi = Debug.MemoryInfo()
        Debug.getMemoryInfo(mi)

        val javaPss = mi.getMemoryStat("summary.java-heap")?.toIntOrNull() ?: 0
        val nativePss = mi.getMemoryStat("summary.native-heap")?.toIntOrNull() ?: 0
        val codePss = mi.getMemoryStat("summary.code")?.toIntOrNull() ?: 0
        val stackPss = mi.getMemoryStat("summary.stack")?.toIntOrNull() ?: 0
        val graphicsPss = mi.getMemoryStat("summary.graphics")?.toIntOrNull() ?: 0
        val systemPss = mi.getMemoryStat("summary.system")?.toIntOrNull() ?: 0

        sb.append("\n  PSS: Java ${javaPss / 1024}MB  Native ${nativePss / 1024}MB")
        sb.append("  Code ${codePss / 1024}MB  Stack ${stackPss / 1024}MB")
        sb.append("  Graphics ${graphicsPss / 1024}MB  System ${systemPss / 1024}MB")
    }

    private fun appendStoreSnapshots(sb: StringBuilder) {
        val snapshots = storeSnapshotProvider?.invoke() ?: return
        if (snapshots.isEmpty()) return

        for ((storeName, fields) in snapshots) {
            val name = storeName.removeSuffix("Store").let { "${it}Store" }
            sb.append("\n  $name")

            // 按路径前缀建立父子关系，父 = 最长匹配前缀
            val paths = fields.keys.toList()
            val childrenOf = mutableMapOf<String?, MutableList<String>>()
            for (path in paths) {
                var parent: String? = null
                for (other in paths) {
                    if (other == path) continue
                    if (path.startsWith("$other.") || path.startsWith("$other[].")) {
                        if (parent == null || other.length > parent.length) parent = other
                    }
                }
                childrenOf.getOrPut(parent) { mutableListOf() }.add(path)
            }

            fun render(parentPath: String?, indent: String) {
                val kids = childrenOf[parentPath] ?: return
                for ((i, childPath) in kids.withIndex()) {
                    val last = i == kids.lastIndex
                    val label = (if (parentPath != null) childPath.removePrefix(parentPath) else childPath)
                        .removePrefix("[].").removePrefix(".")
                    sb.append("\n$indent${if (last) "└─" else "├─"} $label: ${fields[childPath]}")
                    render(childPath, indent + if (last) "   " else "│  ")
                }
            }

            render(null, "  ")
        }
    }
}
