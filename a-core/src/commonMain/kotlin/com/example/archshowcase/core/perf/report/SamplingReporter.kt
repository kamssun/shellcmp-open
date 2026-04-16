package com.example.archshowcase.core.perf.report

import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.StartupTrace
import kotlin.random.Random

class SamplingReporter(
    private val delegate: PerfReporter,
    private val samplingRate: Float,
    private val random: Random = Random.Default
) : PerfReporter {

    private fun shouldReport(): Boolean = samplingRate >= 1.0f || random.nextFloat() < samplingRate

    override fun reportStartup(trace: StartupTrace) {
        if (shouldReport()) delegate.reportStartup(trace)
    }

    override fun reportJank(event: JankEvent) {
        if (shouldReport()) delegate.reportJank(event)
    }

    override fun reportPageMetrics(metrics: PageFrameMetrics) {
        if (shouldReport()) delegate.reportPageMetrics(metrics)
    }

    override fun reportProfileStatus(status: String) {
        delegate.reportProfileStatus(status)
    }
}
