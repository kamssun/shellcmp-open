package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.compose.exposure.ExposureTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsSetupTest {

    private val testDispatcher = StandardTestDispatcher()
    private val reported = mutableListOf<List<AnalyticsEvent>>()
    private val fakeReporter = AnalyticsReporter { events -> reported.add(events.toList()) }

    @BeforeTest
    fun setUp() {
        reported.clear()
        AnalyticsSetup.stop()
        AnalyticsCollector.reset()
        ExposureTracker.reset()
        EventSampler.reset()
    }

    @AfterTest
    fun tearDown() {
        AnalyticsSetup.stop()
        AnalyticsCollector.reset()
        ExposureTracker.reset()
        EventSampler.reset()
    }

    @Test
    fun start_initializesSubsystems() = runTest(testDispatcher) {
        val mapper: (String, Any) -> TrackingEvent? = { _, _ -> null }
        val extractor: (Any) -> Map<String, String> = { emptyMap() }

        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(reporter = fakeReporter, batchSize = 1),
            eventMapper = mapper,
            paramsExtractor = extractor,
        )

        // AnalyticsCollector.eventMapper wired
        assertEquals(mapper, AnalyticsCollector.eventMapper)
        // ExposureTracker.paramsExtractor wired
        assertEquals(extractor, ExposureTracker.paramsExtractor)
        // ExposureTracker.onExposure wired
        assertNotNull(ExposureTracker.onExposure)
    }

    @Test
    fun start_isIdempotent() = runTest(testDispatcher) {
        val mapper1: (String, Any) -> TrackingEvent? = { _, _ -> null }
        val mapper2: (String, Any) -> TrackingEvent? = { s, _ -> TrackingEvent(s, emptyMap()) }

        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(reporter = fakeReporter, batchSize = 1),
            eventMapper = mapper1,
        )
        // Second call with different mapper should be ignored
        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(reporter = fakeReporter, batchSize = 1),
            eventMapper = mapper2,
        )

        assertEquals(mapper1, AnalyticsCollector.eventMapper)
    }

    @Test
    fun stop_resetsSubsystems() = runTest(testDispatcher) {
        val extractor: (Any) -> Map<String, String> = { emptyMap() }

        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(reporter = fakeReporter, batchSize = 100),
            paramsExtractor = extractor,
        )
        assertNotNull(ExposureTracker.onExposure)

        AnalyticsSetup.stop()

        // AnalyticsCollector.eventMapper cleared by reset
        assertNull(AnalyticsCollector.eventMapper)
        // ExposureTracker callbacks cleared by reset
        assertNull(ExposureTracker.onExposure)
        assertNull(ExposureTracker.paramsExtractor)
    }

    @Test
    fun stop_flushesBufferedEvents() = runTest(testDispatcher) {
        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(
                reporter = fakeReporter,
                batchSize = 100, // large batch so collect won't auto-flush
            ),
        )
        AnalyticsCollector.collect(
            AnalyticsEvent.Exposure(
                route = "TestRoute",
                componentType = "Card",
                exposureKey = "card_1",
            )
        )
        assertTrue(reported.isEmpty(), "should not auto-flush yet")

        AnalyticsSetup.stop()

        assertEquals(1, reported.size, "stop() should flush buffered events")
    }

    @Test
    fun stop_whenNotStarted_doesNothing() {
        // Should not throw or cause side effects
        AnalyticsSetup.stop()
        assertNull(AnalyticsCollector.eventMapper)
        assertNull(ExposureTracker.onExposure)
    }

    @Test
    fun startStopStart_cycleWorksCorrectly() = runTest(testDispatcher) {
        val mapper1: (String, Any) -> TrackingEvent? = { _, _ -> null }

        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(reporter = fakeReporter, batchSize = 1),
            eventMapper = mapper1,
        )
        assertEquals(mapper1, AnalyticsCollector.eventMapper)

        AnalyticsSetup.stop()
        assertNull(AnalyticsCollector.eventMapper)

        // Re-start with a new mapper
        val mapper2: (String, Any) -> TrackingEvent? = { s, _ -> TrackingEvent(s, emptyMap()) }
        val extractor: (Any) -> Map<String, String> = { mapOf("k" to "v") }

        AnalyticsSetup.start(
            scope = this,
            config = AnalyticsConfig(reporter = fakeReporter, batchSize = 1),
            eventMapper = mapper2,
            paramsExtractor = extractor,
        )

        assertEquals(mapper2, AnalyticsCollector.eventMapper)
        assertEquals(extractor, ExposureTracker.paramsExtractor)
        assertNotNull(ExposureTracker.onExposure)
    }
}
