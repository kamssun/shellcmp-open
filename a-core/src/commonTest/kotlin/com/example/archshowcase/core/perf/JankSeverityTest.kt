package com.example.archshowcase.core.perf

import com.example.archshowcase.core.perf.model.JankSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JankSeverityTest {

    @Test
    fun `0 dropped frames returns null`() {
        assertNull(JankSeverity.fromDroppedFrames(0))
    }

    @Test
    fun `1 dropped frame returns null`() {
        assertNull(JankSeverity.fromDroppedFrames(1))
    }

    @Test
    fun `2 dropped frames returns SLIGHT`() {
        assertEquals(JankSeverity.SLIGHT, JankSeverity.fromDroppedFrames(2))
    }

    @Test
    fun `3 dropped frames returns SLIGHT`() {
        assertEquals(JankSeverity.SLIGHT, JankSeverity.fromDroppedFrames(3))
    }

    @Test
    fun `4 dropped frames returns MODERATE`() {
        assertEquals(JankSeverity.MODERATE, JankSeverity.fromDroppedFrames(4))
    }

    @Test
    fun `8 dropped frames returns MODERATE`() {
        assertEquals(JankSeverity.MODERATE, JankSeverity.fromDroppedFrames(8))
    }

    @Test
    fun `9 dropped frames returns SEVERE`() {
        assertEquals(JankSeverity.SEVERE, JankSeverity.fromDroppedFrames(9))
    }

    @Test
    fun `25 dropped frames returns SEVERE`() {
        assertEquals(JankSeverity.SEVERE, JankSeverity.fromDroppedFrames(25))
    }

    @Test
    fun `26 dropped frames returns FROZEN`() {
        assertEquals(JankSeverity.FROZEN, JankSeverity.fromDroppedFrames(26))
    }

    @Test
    fun `100 dropped frames returns FROZEN`() {
        assertEquals(JankSeverity.FROZEN, JankSeverity.fromDroppedFrames(100))
    }
}
