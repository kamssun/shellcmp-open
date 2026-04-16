package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.analytics.model.GestureType
import com.example.archshowcase.core.analytics.model.PageAction
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsConfigTest {

    @Test
    fun samplingRate_intentEvent_returnsConfigured() {
        val config = AnalyticsConfig(samplingRules = mapOf("Intent" to 0.8f))
        val event = AnalyticsEvent.Intent(
            route = "test", storeName = "S", intentName = "I", gestureType = GestureType.TAP
        )
        assertEquals(0.8f, config.samplingRate(event))
    }

    @Test
    fun samplingRate_pageEvent_returnsConfigured() {
        val config = AnalyticsConfig(samplingRules = mapOf("Page" to 0.5f))
        val event = AnalyticsEvent.Page(route = "test", action = PageAction.ENTER)
        assertEquals(0.5f, config.samplingRate(event))
    }

    @Test
    fun samplingRate_unknownType_defaultsTo1() {
        val config = AnalyticsConfig(samplingRules = emptyMap())
        val event = AnalyticsEvent.Intent(
            route = "test", storeName = "S", intentName = "I", gestureType = GestureType.TAP
        )
        assertEquals(1.0f, config.samplingRate(event))
    }

}
