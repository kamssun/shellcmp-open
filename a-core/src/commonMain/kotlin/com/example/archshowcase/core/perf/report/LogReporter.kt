package com.example.archshowcase.core.perf.report

import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.Phase
import com.example.archshowcase.core.perf.model.StartupTrace
import com.example.archshowcase.core.util.Log

private const val TAG = "PERF"

private fun roundToOneDecimal(value: Float): String {
    val rounded = (value * 10).toInt() / 10f
    return rounded.toString()
}

class LogReporter : PerfReporter {

    private var lastJankFrameIndex: Long = 0

    override fun reportStartup(trace: StartupTrace) {
        val tree = buildPhaseTree(trace.phases)
        val lines = StringBuilder()
        val hz = trace.deviceInfo.refreshRate.toInt()
        lines.append("[PERF:STARTUP] ${trace.type} ${trace.totalDurationMs}ms (${hz}Hz)")
        renderTree(tree, lines, prefix = "  ")
        Log.i(TAG) { lines.toString() }
    }

    private class PhaseNode(
        val phase: Phase,
        val children: MutableList<PhaseNode> = mutableListOf()
    )

    private fun buildPhaseTree(phases: List<Phase>): List<PhaseNode> {
        val sorted = phases.sortedWith(compareBy<Phase> { it.startMs }.thenByDescending { it.endMs })
        val roots = mutableListOf<PhaseNode>()
        for (phase in sorted) {
            val node = PhaseNode(phase)
            if (!insertInto(roots, node)) roots.add(node)
        }
        return roots
    }

    private fun insertInto(nodes: MutableList<PhaseNode>, node: PhaseNode): Boolean {
        for (i in nodes.indices.reversed()) {
            val candidate = nodes[i]
            if (candidate.phase.startMs <= node.phase.startMs &&
                candidate.phase.endMs >= node.phase.endMs
            ) {
                if (!insertInto(candidate.children, node)) {
                    candidate.children.add(node)
                }
                return true
            }
        }
        return false
    }

    private fun renderTree(
        nodes: List<PhaseNode>,
        sb: StringBuilder,
        prefix: String
    ) {
        for ((i, node) in nodes.withIndex()) {
            val isLast = i == nodes.lastIndex
            val connector = if (isLast) "└─ " else "├─ "
            val childPrefix = if (isLast) "$prefix   " else "$prefix│  "
            val meta = if (node.phase.metadata.isNotEmpty()) {
                " (${node.phase.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
            } else ""
            sb.append("\n$prefix$connector${node.phase.name} ${node.phase.durationMs}ms$meta")
            renderTree(node.children, sb, childPrefix)
        }
    }

    override fun reportJank(event: JankEvent) {
        val prevFrameIndex = lastJankFrameIndex
        val ctx = event.context
        if (ctx.frameIndex > 0) lastJankFrameIndex = ctx.frameIndex

        val build = { formatJankMessage(event, prevFrameIndex) }
        when (event.severity) {
            JankSeverity.SLIGHT -> Log.d(TAG, build)
            JankSeverity.MODERATE -> Log.w(TAG, build)
            JankSeverity.SEVERE, JankSeverity.FROZEN -> Log.e(TAG, message = build)
        }
    }

    private fun formatJankMessage(event: JankEvent, prevFrameIndex: Long): String {
        val ctx = event.context
        val sb = StringBuilder()
        val frameStr = if (ctx.frameIndex > 0) {
            val delta = if (prevFrameIndex > 0) " (+${ctx.frameIndex - prevFrameIndex})" else ""
            " #${ctx.frameIndex}$delta"
        } else ""
        sb.append("[PERF:JANK] ${event.severity} ${event.durationMs.toInt()}ms (${event.droppedFrames} dropped) route=${ctx.activeRoute}$frameStr")
        ctx.transitionInfo?.let { sb.append(" ${it.fromRoute}->${it.toRoute}") }

        // messages + diagnostics + intents 作为子项（帧分解由 FrameMetrics 异步输出）
        val messages = ctx.message?.split("; ") ?: emptyList()
        val diag = ctx.diagnostics
        val items = mutableListOf<String>()
        for (m in messages) {
            // Choreographer → Choreographer.doFrame（对齐 btrace）
            val text = if (m.startsWith("Choreographer")) m.replaceFirst("Choreographer", "Choreographer.doFrame") else m
            items.add(text)
        }
        if (diag.hasGc) {
            val concurrent = diag.gcCount - diag.blockingGcCount
            val parts = mutableListOf<String>()
            if (diag.blockingGcCount > 0) parts.add("${diag.blockingGcCount}x blocking ${diag.blockingGcTimeMs}ms")
            if (concurrent > 0) parts.add("${concurrent}x concurrent ${diag.gcTimeMs - diag.blockingGcTimeMs}ms")
            items.add("gc: ${parts.joinToString(", ")}")
        }
        if (diag.wallTimeMs > 0) items.add("cpu: ${diag.cpuTimeMs}ms/${diag.wallTimeMs}ms (${diag.cpuUsagePercent}%)")
        if (ctx.recentIntents.isNotEmpty()) {
            items.add("intents: ${ctx.recentIntents.joinToString(", ") { it.intent }}")
        }
        for ((i, item) in items.withIndex()) {
            val connector = if (i == items.lastIndex) "└─" else "├─"
            sb.append("\n  $connector $item")
        }
        return sb.toString()
    }

    override fun reportProfileStatus(status: String) {
        Log.i(TAG) { "[PERF:PROFILE] status=$status" }
    }

    override fun reportPageMetrics(metrics: PageFrameMetrics) {
        val jank = metrics.jankCounts.entries.joinToString(",") { "${it.key.name.lowercase()}:${it.value}" }
        val jankStr = if (jank.isNotEmpty()) " jank={$jank}" else ""
        val transStr = metrics.transitionDurationMs?.let { " transition=${it}ms" } ?: ""
        Log.i(TAG) {
            "[PERF:PAGE] route=${metrics.route} stay=${metrics.durationMs}ms " +
                "fps=${roundToOneDecimal(metrics.avgFps)}/${metrics.deviceInfo.refreshRate.toInt()} " +
                "frames=${metrics.totalFrames} dropped=${metrics.droppedFrames}$jankStr$transStr"
        }
    }
}
