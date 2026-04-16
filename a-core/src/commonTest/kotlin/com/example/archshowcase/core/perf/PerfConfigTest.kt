package com.example.archshowcase.core.perf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerfConfigTest {

    @Test
    fun `default config has all features enabled`() {
        val config = PerfConfig()
        assertTrue(config.enabled)
        assertTrue(config.startupTracingEnabled)
        assertTrue(config.jankTrackingEnabled)
        assertTrue(config.contextCollectionEnabled)
        assertTrue(config.fullStateOnSevereJank)
    }

    @Test
    fun `default sampling rate is full`() {
        val config = PerfConfig()
        assertEquals(1.0f, config.samplingRate)
    }

    @Test
    fun `default reporters has LogReporter`() {
        val config = PerfConfig()
        assertEquals(1, config.reporters.size)
        assertTrue(config.reporters.first() is com.example.archshowcase.core.perf.report.LogReporter)
    }
}
