package com.example.archshowcase.core.perf

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.model.StartupTrace
import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.report.PerfReporter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerfMonitorTest {

    private val reportedPages = mutableListOf<PageFrameMetrics>()

    private val collectingReporter = object : PerfReporter {
        override fun reportStartup(trace: StartupTrace) {}
        override fun reportJank(event: JankEvent) {}
        override fun reportPageMetrics(metrics: PageFrameMetrics) {
            reportedPages.add(metrics)
        }
    }

    @BeforeTest
    fun setup() {
        PerfMonitor.reset()
    }

    @AfterTest
    fun cleanup() {
        PerfMonitor.reset()
        reportedPages.clear()
    }

    @Test
    fun `start creates pageTracker`() {
        PerfMonitor.start(PerfConfig(jankTrackingEnabled = false))
        assertNotNull(PerfMonitor.pageTracker)
    }

    @Test
    fun `stop clears pageTracker`() {
        PerfMonitor.start(PerfConfig(jankTrackingEnabled = false))
        PerfMonitor.stop()
        assertNull(PerfMonitor.pageTracker)
    }

    @Test
    fun `notifyTransitionStart updates pageTracker transition`() {
        PerfMonitor.start(PerfConfig(jankTrackingEnabled = false))
        PerfMonitor.pageTracker?.start("Home")

        PerfMonitor.notifyTransitionStart("Home", "Detail")

        assertNotNull(PerfMonitor.pageTracker?.currentTransition)
        assertEquals("Home", PerfMonitor.pageTracker?.currentTransition?.fromRoute)
        assertEquals("Detail", PerfMonitor.pageTracker?.currentTransition?.toRoute)
    }

    @Test
    fun `notifyTransitionStart is no-op when not running`() {
        PerfMonitor.notifyTransitionStart("Home", "Detail")
        // no crash, no pageTracker
        assertNull(PerfMonitor.pageTracker)
    }

    @Test
    fun `trackPage registers doOnResume that calls onTransitionEnd`() {
        PerfMonitor.start(PerfConfig(jankTrackingEnabled = false))
        PerfMonitor.pageTracker?.start("Home")
        PerfMonitor.notifyTransitionStart("Home", "Detail")

        val lifecycle = LifecycleRegistry()
        val ctx = DefaultComponentContext(lifecycle = lifecycle)

        PerfMonitor.trackPage(ctx, "Detail")

        // transition still active before resume
        assertNotNull(PerfMonitor.pageTracker?.currentTransition)

        lifecycle.create()
        lifecycle.resume()

        // after resume, transition should be ended
        assertNull(PerfMonitor.pageTracker?.currentTransition)
        assertEquals("Detail", PerfMonitor.pageTracker?.currentRoute)
    }

    @Test
    fun `trackPage registers doOnPause that reports page metrics`() {
        PerfMonitor.start(PerfConfig(
            jankTrackingEnabled = false,
            reporters = listOf(collectingReporter)
        ))
        PerfMonitor.pageTracker?.start("Home")

        val lifecycle = LifecycleRegistry()
        val ctx = DefaultComponentContext(lifecycle = lifecycle)

        PerfMonitor.trackPage(ctx, "Home")
        lifecycle.create()
        lifecycle.resume()

        // simulate some frames
        val timing = com.example.archshowcase.core.perf.model.FrameTiming(0, 16.6f, 16.6f)
        repeat(5) { PerfMonitor.pageTracker?.onFrame(timing) }

        lifecycle.pause()

        assertEquals(1, reportedPages.size)
        assertEquals("Home", reportedPages[0].route)
        assertEquals(5, reportedPages[0].totalFrames)
    }

    @Test
    fun `trackPage doOnPause skips empty page`() {
        PerfMonitor.start(PerfConfig(
            jankTrackingEnabled = false,
            reporters = listOf(collectingReporter)
        ))
        PerfMonitor.pageTracker?.start("Home")

        val lifecycle = LifecycleRegistry()
        val ctx = DefaultComponentContext(lifecycle = lifecycle)

        PerfMonitor.trackPage(ctx, "Home")
        lifecycle.create()
        lifecycle.resume()

        // no frames
        lifecycle.pause()

        assertTrue(reportedPages.isEmpty())
    }
}
