package com.example.archshowcase.core.perf.model

data class JankEvent(
    val severity: JankSeverity,
    val droppedFrames: Int,
    val durationMs: Float,
    val expectedFrameMs: Float,
    val context: JankContext,
    val deviceInfo: DeviceInfo,
    val timestamp: Long
)
