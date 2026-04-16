package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.report.LogReporter
import com.example.archshowcase.core.perf.report.PerfReporter

data class PerfConfig(
    val enabled: Boolean = true,
    val startupTracingEnabled: Boolean = true,
    val jankTrackingEnabled: Boolean = true,
    val contextCollectionEnabled: Boolean = true,
    val fullStateOnSevereJank: Boolean = true,
    val samplingRate: Float = 1.0f,
    val reporters: List<PerfReporter> = listOf(LogReporter())
)
