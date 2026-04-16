package com.example.archshowcase.core.perf.model

data class StartupTrace(
    val type: StartupType,
    val totalDurationMs: Long,
    val phases: List<Phase>,
    val deviceInfo: DeviceInfo,
    val timestamp: Long
)
