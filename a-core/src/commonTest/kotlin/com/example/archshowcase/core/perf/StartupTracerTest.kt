package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.model.DeviceInfo
import com.example.archshowcase.core.perf.model.StartupType
import com.example.archshowcase.core.perf.startup.StartupTracer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartupTracerTest {

    private val testDevice = DeviceInfo("Test", "Test OS", 4096, 60f)

    @AfterTest
    fun cleanup() {
        StartupTracer.reset()
    }

    @Test
    fun `begin sets isActive to true`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 100)
        assertTrue(StartupTracer.isActive)
    }

    @Test
    fun `duplicate begin is ignored`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 100)
        StartupTracer.begin(StartupType.WARM, startTimeMs = 200)
        val trace = StartupTracer.finish(testDevice, endTimeMs = 500)
        assertNotNull(trace)
        assertEquals(StartupType.COLD, trace.type)
        assertEquals(400, trace.totalDurationMs)
    }

    @Test
    fun `finish returns null when not active`() {
        assertNull(StartupTracer.finish(testDevice))
    }

    @Test
    fun `finish sets isActive to false`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        StartupTracer.finish(testDevice, endTimeMs = 100)
        assertFalse(StartupTracer.isActive)
    }

    @Test
    fun `phases are correctly recorded`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        StartupTracer.markPhaseStart("di_init", timeMs = 10)
        StartupTracer.markPhaseEnd("di_init", timeMs = 50)
        StartupTracer.markPhaseStart("sdk_init", timeMs = 50)
        StartupTracer.markPhaseEnd("sdk_init", timeMs = 120)
        val trace = StartupTracer.finish(testDevice, endTimeMs = 200)

        assertNotNull(trace)
        assertEquals(2, trace.phases.size)
        assertEquals("di_init", trace.phases[0].name)
        assertEquals(40, trace.phases[0].durationMs)
        assertEquals("sdk_init", trace.phases[1].name)
        assertEquals(70, trace.phases[1].durationMs)
    }

    @Test
    fun `nested sub-phases work correctly`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        StartupTracer.markPhaseStart("sdk_init", timeMs = 50)
        StartupTracer.markPhaseStart("sdk_auth", timeMs = 50)
        StartupTracer.markPhaseEnd("sdk_auth", timeMs = 100)
        StartupTracer.markPhaseStart("sdk_im", timeMs = 100)
        StartupTracer.markPhaseEnd("sdk_im", timeMs = 150)
        StartupTracer.markPhaseEnd("sdk_init", timeMs = 150)
        val trace = StartupTracer.finish(testDevice, endTimeMs = 200)

        assertNotNull(trace)
        assertEquals(3, trace.phases.size)
        assertEquals("sdk_auth", trace.phases[0].name)
        assertEquals("sdk_im", trace.phases[1].name)
        assertEquals("sdk_init", trace.phases[2].name)
        assertEquals(100, trace.phases[2].durationMs)
    }

    @Test
    fun `metadata is preserved`() {
        StartupTracer.begin(StartupType.WARM, startTimeMs = 0)
        StartupTracer.markPhaseStart("sdk_auth", timeMs = 10)
        StartupTracer.markPhaseEnd("sdk_auth", timeMs = 60, metadata = mapOf("version" to "3.0"))
        val trace = StartupTracer.finish(testDevice, endTimeMs = 100)

        assertNotNull(trace)
        assertEquals("3.0", trace.phases[0].metadata["version"])
    }

    @Test
    fun `markPhaseEnd without start is ignored`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        StartupTracer.markPhaseEnd("unknown", timeMs = 50)
        val trace = StartupTracer.finish(testDevice, endTimeMs = 100)
        assertNotNull(trace)
        assertTrue(trace.phases.isEmpty())
    }

    @Test
    fun `traced records phase and returns block result`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        val result = StartupTracer.traced("di_init") { 42 }
        assertEquals(42, result)
        val trace = StartupTracer.finish(testDevice, endTimeMs = 100)
        assertNotNull(trace)
        assertEquals(1, trace.phases.size)
        assertEquals("di_init", trace.phases[0].name)
    }

    @Test
    fun `traced records phase even when block throws`() {
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        try {
            StartupTracer.traced("failing") { error("boom") }
        } catch (_: IllegalStateException) { }
        val trace = StartupTracer.finish(testDevice, endTimeMs = 100)
        assertNotNull(trace)
        assertEquals(1, trace.phases.size)
        assertEquals("failing", trace.phases[0].name)
    }

    @Test
    fun `traced nesting produces correct phase order`() {
        StartupTracer.reset()
        assertFalse(StartupTracer.isActive, "should be inactive after reset")
        StartupTracer.begin(StartupType.COLD, startTimeMs = 0)
        assertTrue(StartupTracer.isActive, "should be active after begin")
        StartupTracer.traced("outer") {
            StartupTracer.traced("inner_a") { }
            StartupTracer.traced("inner_b") { }
        }
        assertTrue(StartupTracer.isActive, "should still be active after traced")
        val trace = StartupTracer.finish(testDevice, endTimeMs = 100)
        assertNotNull(trace)
        val names = trace.phases.map { it.name }
        assertEquals(listOf("inner_a", "inner_b", "outer"), names)
    }
}
