package com.example.archshowcase.chat.stress

import com.example.archshowcase.core.util.Log
import kotlin.concurrent.Volatile
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * 压测专用日志收集器（零侵入：不修改 ChatDao，从调用侧采集）。
 *
 * - [enabled] = false 时仅多一次 volatile 读（~1ns），零开销
 * - 采集到 ring buffer，定期聚合输出 P50/P95/P99/max
 * - 不依赖 a-core/perf，完全独立
 */
object StressTestLogger {

    private const val TAG = "StressTest"

    @Volatile
    var enabled = false

    private val enqueueMicros = MetricBuffer("ENQUEUE")

    @Volatile private var totalEnqueued = 0L

    private var startMark = TimeSource.Monotonic.markNow()

    // ── 计时 API ──

    fun markStart(): TimeMark? = if (enabled) TimeSource.Monotonic.markNow() else null

    // ── 采集 API（从 MockChatRepository 调用侧记录） ──

    fun recordEnqueue(start: TimeMark?) {
        if (start == null) return
        val elapsedNs = start.elapsedNow().inWholeNanoseconds
        enqueueMicros.add((elapsedNs / 1000).toInt())
        totalEnqueued++
    }

    // ── 输出 API ──

    fun printSnapshot() {
        if (!enabled) return
        val elapsedSec = startMark.elapsedNow().inWholeSeconds
        val total = totalEnqueued
        val eq = enqueueMicros.drain()

        Log.d(TAG) {
            buildString {
                appendLine("──── ${elapsedSec}s Window ────────────────────────────")
                append("  ENQUEUE    ${eq.summary()}  total=$total")
            }
        }
    }

    fun printFinalSummary() {
        if (!enabled) return
        val elapsedMs = startMark.elapsedNow().inWholeMilliseconds
        val total = totalEnqueued
        val throughput = if (elapsedMs > 0) total * 1000 / elapsedMs else 0

        Log.d(TAG) {
            buildString {
                appendLine("═══════════ FINAL SUMMARY ═══════════════════════")
                appendLine("  Duration:      ${elapsedMs / 1000}s")
                appendLine("  Total enqueued: $total")
                appendLine("  Throughput:    $throughput msg/s")
                append("═════════════════════════════════════════════════")
            }
        }
    }

    fun reset() {
        enqueueMicros.clear()
        totalEnqueued = 0
        startMark = TimeSource.Monotonic.markNow()
    }

    // ── Ring Buffer ──

    class MetricBuffer(private val name: String, private val capacity: Int = 10_000) {
        private val values = IntArray(capacity)
        @Volatile private var writeIndex = 0
        @Volatile private var count = 0

        fun add(valueMicros: Int) {
            val idx = writeIndex % capacity
            values[idx] = valueMicros
            writeIndex++
            if (count < capacity) count++
        }

        fun drain(): DrainResult {
            val n = count.coerceAtMost(capacity)
            if (n == 0) return DrainResult(name, 0, 0, 0, 0, 0, 0)
            val wi = writeIndex
            val snapshot = IntArray(n)
            val startIdx = if (wi > capacity) wi - capacity else 0
            for (i in 0 until n) {
                snapshot[i] = values[(startIdx + i) % capacity]
            }
            count = 0
            writeIndex = 0
            snapshot.sort()
            return DrainResult(
                name = name,
                count = n,
                avgMicros = snapshot.map { it.toLong() }.average().toLong(),
                p50Micros = snapshot[n / 2].toLong(),
                p95Micros = snapshot[(n * 0.95).toInt().coerceAtMost(n - 1)].toLong(),
                p99Micros = snapshot[(n * 0.99).toInt().coerceAtMost(n - 1)].toLong(),
                maxMicros = snapshot.last().toLong(),
            )
        }

        fun clear() {
            count = 0
            writeIndex = 0
        }
    }

    data class DrainResult(
        val name: String,
        val count: Int,
        val avgMicros: Long,
        val p50Micros: Long,
        val p95Micros: Long,
        val p99Micros: Long,
        val maxMicros: Long,
    ) {
        fun summary(): String =
            if (count == 0) "[$name: no data]"
            else "[$count] avg=${fmt(avgMicros)} P50=${fmt(p50Micros)} P95=${fmt(p95Micros)} P99=${fmt(p99Micros)} max=${fmt(maxMicros)}"

        private fun fmt(us: Long): String = when {
            us < 1000 -> "${us}μs"
            us < 1_000_000 -> "${us / 1000}.${(us % 1000) / 100}ms"
            else -> "${us / 1_000_000}.${(us % 1_000_000) / 100_000}s"
        }
    }
}
