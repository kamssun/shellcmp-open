package com.example.archshowcase.core.util

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogTest {

    private val captured = mutableListOf<CapturedLog>()

    private val testWriter = LogWriter { severity, tag, message, throwable ->
        captured.add(CapturedLog(severity, tag, message, throwable))
    }

    @AfterTest
    fun tearDown() {
        captured.clear()
        Log.setMinSeverity(LogSeverity.DEBUG)
    }

    // --- Happy path: each severity dispatches correctly ---

    @Test
    fun debugDispatchesSeverityTagMessage() {
        Log.install(testWriter)
        Log.d("T") { "msg" }

        assertEquals(1, captured.size)
        with(captured[0]) {
            assertEquals(LogSeverity.DEBUG, severity)
            assertEquals("T", tag)
            assertEquals("msg", message)
            assertNull(throwable)
        }
    }

    @Test
    fun infoDispatchesSeverityTagMessage() {
        Log.install(testWriter)
        Log.i("T") { "msg" }

        assertEquals(1, captured.size)
        with(captured[0]) {
            assertEquals(LogSeverity.INFO, severity)
            assertEquals("T", tag)
            assertEquals("msg", message)
            assertNull(throwable)
        }
    }

    @Test
    fun warnDispatchesSeverityTagMessage() {
        Log.install(testWriter)
        Log.w("T") { "msg" }

        assertEquals(1, captured.size)
        with(captured[0]) {
            assertEquals(LogSeverity.WARN, severity)
            assertEquals("T", tag)
            assertEquals("msg", message)
            assertNull(throwable)
        }
    }

    @Test
    fun errorDispatchesSeverityTagMessage() {
        Log.install(testWriter)
        Log.e("T") { "msg" }

        assertEquals(1, captured.size)
        with(captured[0]) {
            assertEquals(LogSeverity.ERROR, severity)
            assertEquals("T", tag)
            assertEquals("msg", message)
            assertNull(throwable)
        }
    }

    @Test
    fun errorPropagatesThrowable() {
        Log.install(testWriter)
        val ex = RuntimeException("boom")
        Log.e("T", ex) { "msg" }

        assertEquals(ex, captured[0].throwable)
    }

    // --- Default tag ---

    @Test
    fun defaultTagIsEmpty() {
        Log.install(testWriter)
        Log.d { "d" }
        Log.i { "i" }
        Log.w { "w" }
        Log.e { "e" }

        assertEquals(4, captured.size)
        assertTrue(captured.all { it.tag == "" })
    }

    // --- Multiple writers ---

    @Test
    fun multipleWritersAllReceiveEvents() {
        val captured2 = mutableListOf<CapturedLog>()
        val writer2 = LogWriter { severity, tag, message, throwable ->
            captured2.add(CapturedLog(severity, tag, message, throwable))
        }
        Log.install(testWriter, writer2)
        Log.e("T") { "msg" }

        assertEquals(1, captured.size)
        assertEquals(1, captured2.size)
    }

    // --- minSeverity filtering: each level as boundary ---

    @Test
    fun minSeverityInfoFiltersDebug() {
        Log.install(testWriter)
        Log.setMinSeverity(LogSeverity.INFO)

        Log.d("T") { "filtered" }
        assertTrue(captured.isEmpty(), "DEBUG should be filtered at INFO level")

        Log.i("T") { "pass" }
        Log.w("T") { "pass" }
        Log.e("T") { "pass" }
        assertEquals(3, captured.size, "INFO/WARN/ERROR should pass at INFO level")
    }

    @Test
    fun minSeverityWarnFiltersDebugAndInfo() {
        Log.install(testWriter)
        Log.setMinSeverity(LogSeverity.WARN)

        Log.d("T") { "filtered" }
        Log.i("T") { "filtered" }
        assertTrue(captured.isEmpty(), "DEBUG and INFO should be filtered at WARN level")

        Log.w("T") { "pass" }
        Log.e("T") { "pass" }
        assertEquals(2, captured.size, "WARN/ERROR should pass at WARN level")
    }

    @Test
    fun minSeverityErrorFiltersAllButError() {
        Log.install(testWriter)
        Log.setMinSeverity(LogSeverity.ERROR)

        Log.d("T") { "filtered" }
        Log.i("T") { "filtered" }
        Log.w("T") { "filtered" }
        assertTrue(captured.isEmpty(), "DEBUG/INFO/WARN should be filtered at ERROR level")

        Log.e("T") { "pass" }
        assertEquals(1, captured.size)
        assertEquals(LogSeverity.ERROR, captured[0].severity)
    }

    private data class CapturedLog(
        val severity: LogSeverity,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )
}
