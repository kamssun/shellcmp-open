package com.example.archshowcase.core.perf.model

data class FrameInfo(
    val droppedFrames: Int,
    val durationMs: Float,
    val refreshRate: Float
)
