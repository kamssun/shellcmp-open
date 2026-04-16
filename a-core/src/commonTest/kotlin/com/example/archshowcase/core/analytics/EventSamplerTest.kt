package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.analytics.model.GestureType
import com.example.archshowcase.core.analytics.model.PageAction
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventSamplerTest {

    @BeforeTest
    fun setUp() {
        EventSampler.reset()
    }

    @AfterTest
    fun tearDown() {
        EventSampler.reset()
    }

    @Test
    fun fullRate_alwaysSamples() {
        val config = AnalyticsConfig(samplingRules = mapOf("Intent" to 1.0f))
        val event = AnalyticsEvent.Intent(
            route = "test", storeName = "S", intentName = "I", gestureType = GestureType.TAP
        )
        repeat(100) {
            assertTrue(EventSampler.shouldSample(event, config))
        }
    }

    @Test
    fun zeroRate_neverSamples() {
        val config = AnalyticsConfig(samplingRules = mapOf("Page" to 0.0f))
        val event = AnalyticsEvent.Page(route = "test", action = PageAction.ENTER)
        repeat(100) {
            assertFalse(EventSampler.shouldSample(event, config))
        }
    }

    @Test
    fun halfRate_samplesSome() {
        // 使用固定 seed 确保可重复
        EventSampler.setRandom(Random(42))
        val config = AnalyticsConfig(samplingRules = mapOf("Exposure" to 0.5f))
        val event = AnalyticsEvent.Exposure(
            route = "test", componentType = "AppButton", exposureKey = "k"
        )
        var sampled = 0
        repeat(1000) {
            if (EventSampler.shouldSample(event, config)) sampled++
        }
        // 应该大约 500，允许一定误差
        assertTrue(sampled in 400..600, "Expected ~500 but got $sampled")
    }

    @Test
    fun unknownType_defaultsToFullRate() {
        val config = AnalyticsConfig(samplingRules = emptyMap())
        val event = AnalyticsEvent.Intent(
            route = "test", storeName = "S", intentName = "I", gestureType = GestureType.TAP
        )
        assertTrue(EventSampler.shouldSample(event, config))
    }
}
