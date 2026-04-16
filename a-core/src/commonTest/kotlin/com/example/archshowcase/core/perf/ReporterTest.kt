package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.model.*
import com.example.archshowcase.core.perf.report.CompositeReporter
import com.example.archshowcase.core.perf.report.PerfReporter
import com.example.archshowcase.core.perf.report.SamplingReporter
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ReporterTest {

    private class CountingReporter : PerfReporter {
        var startupCount = 0
        var jankCount = 0
        var pageCount = 0

        override fun reportStartup(trace: StartupTrace) { startupCount++ }
        override fun reportJank(event: JankEvent) { jankCount++ }
        override fun reportPageMetrics(metrics: PageFrameMetrics) { pageCount++ }
    }

    private val testDevice = DeviceInfo("Test", "Test OS", 4096, 60f)
    private val testStartup = StartupTrace(StartupType.COLD, 500, emptyList(), testDevice, 0)
    private val testJank = JankEvent(
        severity = JankSeverity.MODERATE,
        droppedFrames = 5,
        durationMs = 83f,
        expectedFrameMs = 16.6f,
        context = JankContext(
            recentIntents = emptyList(),
            stateSnapshots = emptyMap(),
            fullStates = null,
            activeRoute = "Home",
            transitionInfo = null,
            frameInfo = FrameInfo(5, 83f, 60f),
            memoryInfo = MemoryInfo(100, 200, 50f)
        ),
        deviceInfo = testDevice,
        timestamp = 0
    )
    private val testPage = PageFrameMetrics(
        route = "Home",
        durationMs = 5000,
        totalFrames = 300,
        droppedFrames = 5,
        avgFps = 60f,
        jankCounts = emptyMap(),
        deviceInfo = testDevice
    )

    @Test
    fun `composite reporter calls all delegates`() {
        val r1 = CountingReporter()
        val r2 = CountingReporter()
        val composite = CompositeReporter(listOf(r1, r2))

        composite.reportStartup(testStartup)
        composite.reportJank(testJank)
        composite.reportPageMetrics(testPage)

        assertEquals(1, r1.startupCount)
        assertEquals(1, r2.startupCount)
        assertEquals(1, r1.jankCount)
        assertEquals(1, r2.jankCount)
        assertEquals(1, r1.pageCount)
        assertEquals(1, r2.pageCount)
    }

    @Test
    fun `sampling reporter at rate 0 never reports`() {
        val inner = CountingReporter()
        // Use a fixed random that always returns 0.5
        val sampler = SamplingReporter(inner, 0f, Random(42))

        repeat(100) { sampler.reportStartup(testStartup) }
        assertEquals(0, inner.startupCount)
    }

    @Test
    fun `sampling reporter at rate 1 always reports`() {
        val inner = CountingReporter()
        val sampler = SamplingReporter(inner, 1.0f)

        repeat(10) { sampler.reportStartup(testStartup) }
        assertEquals(10, inner.startupCount)
    }
}
