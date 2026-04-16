package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.FrameTiming
import com.example.archshowcase.core.perf.model.PageFrameMetrics
import com.example.archshowcase.core.perf.page.PageTracker
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageTrackerTest {

    private val testDevice = DeviceInfo("Test", "Test OS", 4096, 60f)
    private val reportedMetrics = mutableListOf<PageFrameMetrics>()
    private val tracker = PageTracker(
        deviceInfoProvider = { testDevice },
        onPageMetrics = { reportedMetrics.add(it) }
    )

    @AfterTest
    fun cleanup() {
        tracker.reset()
        reportedMetrics.clear()
    }

    @Test
    fun `start sets current route`() {
        tracker.start("Home")
        assertEquals("Home", tracker.currentRoute)
    }

    @Test
    fun `transition start sets transition info`() {
        tracker.start("Home")
        tracker.onTransitionStart("Home", "Detail")
        assertNotNull(tracker.currentTransition)
        assertEquals("Home", tracker.currentTransition?.fromRoute)
        assertEquals("Detail", tracker.currentTransition?.toRoute)
    }

    @Test
    fun `transition end clears transition and updates route`() {
        tracker.start("Home")
        tracker.onTransitionStart("Home", "Detail")
        tracker.onTransitionEnd("Detail")
        assertNull(tracker.currentTransition)
        assertEquals("Detail", tracker.currentRoute)
    }

    @Test
    fun `transition start flushes previous page metrics`() {
        tracker.start("Home")
        tracker.onFrame(FrameTiming(0, 16.6f, 16.6f))
        tracker.onFrame(FrameTiming(16, 16.6f, 16.6f))
        tracker.onTransitionStart("Home", "Detail")
        assertEquals(1, reportedMetrics.size)
        assertEquals("Home", reportedMetrics[0].route)
        assertEquals(2, reportedMetrics[0].totalFrames)
    }

    @Test
    fun `frame counting works correctly`() {
        tracker.start("Home")
        repeat(10) { tracker.onFrame(FrameTiming(0, 16.6f, 16.6f)) }
        // 1 frame with 3 dropped
        tracker.onFrame(FrameTiming(0, 66.4f, 16.6f))
        val metrics = tracker.onPagePaused()
        assertNotNull(metrics)
        assertEquals(11, metrics.totalFrames)
        assertEquals(3, metrics.droppedFrames)
    }

    @Test
    fun `onPagePaused returns null when no route set`() {
        assertNull(tracker.onPagePaused())
    }
}
