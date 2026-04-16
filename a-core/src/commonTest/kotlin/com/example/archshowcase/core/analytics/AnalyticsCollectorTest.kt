package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.analytics.model.GestureType
import com.example.archshowcase.core.analytics.model.PageAction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsCollectorTest {

    private val reported = mutableListOf<List<AnalyticsEvent>>()
    private val fakeReporter = AnalyticsReporter { events -> reported.add(events.toList()) }

    @BeforeTest
    fun setUp() {
        reported.clear()
        AnalyticsCollector.reset()
        EventSampler.reset()
    }

    @AfterTest
    fun tearDown() {
        AnalyticsCollector.reset()
        EventSampler.reset()
    }

    @Test
    fun collect_reachesBatchSize_triggersFlush() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                reporter = fakeReporter,
                batchSize = 3,
            )
        )
        repeat(3) { i ->
            AnalyticsCollector.collect(makeIntentEvent("store_$i"))
        }
        assertEquals(1, reported.size)
        assertEquals(3, reported[0].size)
    }

    @Test
    fun collect_belowBatchSize_noFlush() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                reporter = fakeReporter,
                batchSize = 10,
            )
        )
        AnalyticsCollector.collect(makeIntentEvent("store_1"))
        assertTrue(reported.isEmpty())
    }

    @Test
    fun flush_drainsBuffer() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                reporter = fakeReporter,
                batchSize = 100,
            )
        )
        AnalyticsCollector.collect(makeIntentEvent("store_1"))
        AnalyticsCollector.collect(makeIntentEvent("store_2"))
        AnalyticsCollector.flush()
        assertEquals(1, reported.size)
        assertEquals(2, reported[0].size)
    }

    @Test
    fun disabled_collectDoesNothing() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                enabled = false,
                reporter = fakeReporter,
                batchSize = 1,
            )
        )
        AnalyticsCollector.collect(makeIntentEvent("store_1"))
        assertTrue(reported.isEmpty())
    }

    @Test
    fun ringBuffer_dropsOldestWhenFull() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                reporter = fakeReporter,
                bufferCapacity = 3,
                batchSize = 100, // 不自动 flush
            )
        )
        repeat(5) { i ->
            AnalyticsCollector.collect(makeIntentEvent("store_$i"))
        }
        AnalyticsCollector.flush()
        assertEquals(1, reported.size)
        assertEquals(3, reported[0].size)
        // 最新 3 个保留
        val names = reported[0].map { (it as AnalyticsEvent.Intent).storeName }
        assertEquals(listOf("store_2", "store_3", "store_4"), names)
    }

    @Test
    fun collect_enrichesSessionId() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                reporter = fakeReporter,
                batchSize = 1,
            )
        )
        AnalyticsCollector.collect(makeIntentEvent("store_1"))
        assertEquals(1, reported.size)
        val event = reported[0][0] as AnalyticsEvent.Intent
        assertTrue(event.sessionId.isNotEmpty(), "sessionId should be enriched")
    }

    @Test
    fun collect_flushIntervalElapsed_triggersFlush() {
        AnalyticsCollector.start(
            AnalyticsConfig(
                reporter = fakeReporter,
                batchSize = 100, // won't trigger by size
                flushIntervalMs = 0, // always elapsed
            )
        )
        AnalyticsCollector.collect(makeIntentEvent("store_1"))
        assertEquals(1, reported.size, "should flush by time interval")
        assertEquals(1, reported[0].size)
    }

    private fun makeIntentEvent(storeName: String) = AnalyticsEvent.Intent(
        route = "TestRoute",
        storeName = storeName,
        intentName = "TestIntent",
        gestureType = GestureType.TAP,
    )
}
