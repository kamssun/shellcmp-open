package com.example.archshowcase.core.perf.model

data class FrameTiming(
    val frameStartMs: Long,
    val frameDurationMs: Float,
    val expectedDurationMs: Float
) {
    val droppedFrames: Int
        get() = (frameDurationMs / expectedDurationMs).toInt() - 1
}
