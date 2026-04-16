package com.example.archshowcase.core.perf.startup

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.Phase
import com.example.archshowcase.core.perf.model.StartupTrace
import com.example.archshowcase.core.perf.model.StartupType
import com.example.archshowcase.core.perf.platform.currentUptimeMs

object StartupTracer {

    private var startType: StartupType? = null
    private var startMs: Long = 0L
    private val phases = mutableListOf<Phase>()
    private val openPhases = mutableMapOf<String, Long>()

    var isActive: Boolean = false
        private set

    fun begin(type: StartupType, startTimeMs: Long = currentUptimeMs()) {
        if (isActive) return
        isActive = true
        startType = type
        startMs = startTimeMs
        phases.clear()
        openPhases.clear()
    }

    fun markPhaseStart(name: String, timeMs: Long = currentUptimeMs()) {
        if (!isActive) return
        openPhases[name] = timeMs
    }

    fun markPhaseEnd(
        name: String,
        timeMs: Long = currentUptimeMs(),
        metadata: Map<String, String> = emptyMap()
    ) {
        if (!isActive) return
        val phaseStart = openPhases.remove(name) ?: return
        phases.add(Phase(name = name, startMs = phaseStart, endMs = timeMs, metadata = metadata))
    }

    fun finish(deviceInfo: DeviceInfo, endTimeMs: Long = currentUptimeMs()): StartupTrace? {
        if (!isActive) return null
        isActive = false
        val type = startType ?: return null
        val trace = StartupTrace(
            type = type,
            totalDurationMs = endTimeMs - startMs,
            phases = phases.toList(),
            deviceInfo = deviceInfo,
            timestamp = endTimeMs
        )
        reset()
        return trace
    }

    inline fun <T> traced(name: String, block: () -> T): T {
        markPhaseStart(name)
        return try {
            block()
        } finally {
            markPhaseEnd(name)
        }
    }

    inline fun <T> traced(
        name: String,
        metadata: Map<String, String>,
        block: () -> T
    ): T {
        markPhaseStart(name)
        return try {
            block()
        } finally {
            markPhaseEnd(name, metadata = metadata)
        }
    }

    internal fun reset() {
        isActive = false
        startType = null
        startMs = 0L
        phases.clear()
        openPhases.clear()
    }
}
