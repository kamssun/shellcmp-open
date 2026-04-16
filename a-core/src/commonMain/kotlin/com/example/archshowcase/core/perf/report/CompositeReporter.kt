package com.example.archshowcase.core.perf.report

import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.StartupTrace

class CompositeReporter(private val reporters: List<PerfReporter>) : PerfReporter {

    override fun reportStartup(trace: StartupTrace) {
        reporters.forEach { it.reportStartup(trace) }
    }

    override fun reportJank(event: JankEvent) {
        reporters.forEach { it.reportJank(event) }
    }

    override fun reportPageMetrics(metrics: PageFrameMetrics) {
        reporters.forEach { it.reportPageMetrics(metrics) }
    }

    override fun reportProfileStatus(status: String) {
        reporters.forEach { it.reportProfileStatus(status) }
    }
}
