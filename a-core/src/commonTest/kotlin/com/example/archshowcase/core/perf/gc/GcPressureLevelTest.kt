package com.example.archshowcase.core.perf.gc

import kotlin.test.Test
import kotlin.test.assertTrue

class GcPressureLevelTest {

    @Test
    fun `levels are ordered WARN SEVERE CRITICAL`() {
        assertTrue(GcPressureLevel.WARN < GcPressureLevel.SEVERE)
        assertTrue(GcPressureLevel.SEVERE < GcPressureLevel.CRITICAL)
    }

    @Test
    fun `SEVERE is greater than or equal to SEVERE`() {
        assertTrue(GcPressureLevel.SEVERE >= GcPressureLevel.SEVERE)
    }

    @Test
    fun `CRITICAL is greater than or equal to SEVERE`() {
        assertTrue(GcPressureLevel.CRITICAL >= GcPressureLevel.SEVERE)
    }

    @Test
    fun `WARN is less than SEVERE`() {
        assertTrue(GcPressureLevel.WARN < GcPressureLevel.SEVERE)
    }
}
