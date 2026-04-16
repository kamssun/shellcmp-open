package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.model.FrameTiming
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameTimingTest {

    @Test
    fun `normal frame at 60Hz has 0 dropped frames`() {
        val timing = FrameTiming(
            frameStartMs = 0,
            frameDurationMs = 16.6f,
            expectedDurationMs = 16.6f
        )
        assertEquals(0, timing.droppedFrames)
    }

    @Test
    fun `double duration at 60Hz has 1 dropped frame`() {
        val timing = FrameTiming(
            frameStartMs = 0,
            frameDurationMs = 33.2f,
            expectedDurationMs = 16.6f
        )
        assertEquals(1, timing.droppedFrames)
    }

    @Test
    fun `5x duration at 60Hz has 4 dropped frames`() {
        val timing = FrameTiming(
            frameStartMs = 0,
            frameDurationMs = 83.0f,
            expectedDurationMs = 16.6f
        )
        assertEquals(4, timing.droppedFrames)
    }

    @Test
    fun `normal frame at 120Hz has 0 dropped frames`() {
        val timing = FrameTiming(
            frameStartMs = 0,
            frameDurationMs = 8.3f,
            expectedDurationMs = 8.3f
        )
        assertEquals(0, timing.droppedFrames)
    }

    @Test
    fun `3x duration at 120Hz has 2 dropped frames`() {
        val timing = FrameTiming(
            frameStartMs = 0,
            frameDurationMs = 24.9f,
            expectedDurationMs = 8.3f
        )
        assertEquals(2, timing.droppedFrames)
    }
}
