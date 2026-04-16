package com.example.archshowcase.core.perf.startup

import com.example.archshowcase.core.perf.model.StartupType
import com.example.archshowcase.core.perf.platform.currentUptimeMs
import com.example.archshowcase.core.perf.platform.processStartTimeMs

object ApplicationStartupHelper {

    private var attachBaseEndMs: Long = 0L

    fun onAttachBaseContextEnd() {
        attachBaseEndMs = currentUptimeMs()
    }

    fun beginStartupTrace() {
        val processStart = processStartTimeMs()
        val contentProviderMs = currentUptimeMs() - attachBaseEndMs
        StartupTracer.begin(StartupType.COLD, startTimeMs = processStart)
        StartupTracer.markPhaseStart("process_fork", timeMs = processStart)
        StartupTracer.markPhaseEnd(
            "process_fork",
            metadata = mapOf("content_provider" to "${contentProviderMs}ms")
        )
    }
}
