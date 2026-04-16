package com.example.archshowcase.core.perf.model

data class PageFrameMetrics(
    val route: String,
    val routeParams: String? = null,
    val durationMs: Long,
    val totalFrames: Int,
    val droppedFrames: Int,
    val avgFps: Float,
    val jankCounts: Map<JankSeverity, Int>,
    val worstJank: JankEvent? = null,
    val transitionDurationMs: Long? = null,
    val deviceInfo: DeviceInfo
)
