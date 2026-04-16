package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.FrameTiming
import com.example.archshowcase.core.perf.model.JankEvent
import com.example.archshowcase.core.perf.model.JankSeverity
import com.example.archshowcase.core.perf.jank.ContextCollector
import com.example.archshowcase.core.perf.jank.JankTracker
import com.example.archshowcase.core.perf.page.PageTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JankTrackerTest {

    private val testDevice = DeviceInfo("Test", "Test OS", 4096, 60f)
    private val events = mutableListOf<JankEvent>()

    private fun createTracker(): JankTracker {
        val pageTracker = PageTracker(
            deviceInfoProvider = { testDevice },
            onPageMetrics = {}
        )
        pageTracker.start("Home")
        val collector = ContextCollector(PerfConfig(contextCollectionEnabled = false))
        val tracker = JankTracker(
            contextCollector = collector,
            pageTracker = pageTracker,
            deviceInfoProvider = { testDevice },
            onJank = { events.add(it) }
        )
        // 消耗 warmup 帧，使后续帧正常检测
        repeat(3) { tracker.onFrame(FrameTiming(0, 16.6f, 16.6f)) }
        return tracker
    }

    @Test
    fun `normal frame does not trigger jank`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 16.6f, 16.6f))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `single dropped frame does not trigger jank`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 33.2f, 16.6f))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `2 dropped frames triggers SLIGHT jank`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 49.8f, 16.6f))
        assertEquals(1, events.size)
        assertEquals(JankSeverity.SLIGHT, events[0].severity)
        assertEquals(2, events[0].droppedFrames)
    }

    @Test
    fun `5 dropped frames triggers MODERATE jank at 60Hz`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 99.6f, 16.6f))
        assertEquals(1, events.size)
        assertEquals(JankSeverity.MODERATE, events[0].severity)
    }

    @Test
    fun `10 dropped frames triggers SEVERE jank`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 182.6f, 16.6f))
        assertEquals(1, events.size)
        assertEquals(JankSeverity.SEVERE, events[0].severity)
    }

    @Test
    fun `30 dropped frames triggers FROZEN jank`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 514.8f, 16.6f))
        assertEquals(1, events.size)
        assertEquals(JankSeverity.FROZEN, events[0].severity)
    }

    @Test
    fun `jank at 120Hz uses correct threshold`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 24.9f, 8.3f))
        assertEquals(1, events.size)
        assertEquals(JankSeverity.SLIGHT, events[0].severity)
    }

    @Test
    fun `jank event includes route`() {
        val tracker = createTracker()
        tracker.onFrame(FrameTiming(0, 49.8f, 16.6f))
        assertEquals("Home", events[0].context.activeRoute)
    }

    @Test
    fun `warmup frames are skipped`() {
        val pageTracker = PageTracker(
            deviceInfoProvider = { testDevice },
            onPageMetrics = {}
        )
        pageTracker.start("Home")
        val collector = ContextCollector(PerfConfig(contextCollectionEnabled = false))
        val tracker = JankTracker(
            contextCollector = collector,
            pageTracker = pageTracker,
            deviceInfoProvider = { testDevice },
            onJank = { events.add(it) }
        )
        // 前 3 帧即使掉帧也不触发
        repeat(3) { tracker.onFrame(FrameTiming(0, 500f, 16.6f)) }
        assertTrue(events.isEmpty())
        // 第 4 帧开始正常检测
        tracker.onFrame(FrameTiming(0, 500f, 16.6f))
        assertEquals(1, events.size)
    }
}
