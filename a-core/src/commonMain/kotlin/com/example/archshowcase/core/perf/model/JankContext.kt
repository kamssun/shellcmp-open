package com.example.archshowcase.core.perf.model

import com.example.archshowcase.core.perf.platform.FrameDiagnostics
import com.example.archshowcase.core.trace.user.IntentRecord

data class JankContext(
    val recentIntents: List<IntentRecord>,
    val stateSnapshots: Map<String, StateDigest>,
    val fullStates: Map<String, String>?,
    val activeRoute: String,
    val transitionInfo: TransitionInfo?,
    val frameInfo: FrameInfo,
    val memoryInfo: MemoryInfo,
    val message: String? = null,
    val diagnostics: FrameDiagnostics = FrameDiagnostics(),
    val frameIndex: Long = 0
)
