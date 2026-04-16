package com.example.archshowcase.core.perf.report

import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.StartupTrace

interface PerfReporter {
    fun reportStartup(trace: StartupTrace)
    fun reportJank(event: JankEvent)
    fun reportPageMetrics(metrics: PageFrameMetrics)
    fun reportProfileStatus(status: String) {}
}
