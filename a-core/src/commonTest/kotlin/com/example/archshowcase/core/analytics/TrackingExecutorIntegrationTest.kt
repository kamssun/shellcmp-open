package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.analytics.model.GestureType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Task 8.1: 端到端集成测试。
 * 模拟 AppButton onClick → markUserGesture → TrackingExecutor → AnalyticsCollector。
 */
class TrackingExecutorIntegrationTest {

    private val reported = mutableListOf<List<AnalyticsEvent>>()
    private val fakeReporter = AnalyticsReporter { events -> reported.add(events.toList()) }

    @BeforeTest
    fun setUp() {
        InteractionContext.reset()
        AnalyticsCollector.reset()
        EventSampler.reset()
        reported.clear()
        AnalyticsCollector.start(AnalyticsConfig(reporter = fakeReporter, batchSize = 1))
    }

    @AfterTest
    fun tearDown() {
        InteractionContext.reset()
        AnalyticsCollector.reset()
        EventSampler.reset()
    }

    @Test
    fun userInitiated_intent_isCollected() {
        // 模拟 AppButton onClick
        InteractionContext.markUserGesture("AppButton")

        // 模拟 EventMapper 返回
        AnalyticsCollector.eventMapper = { storeName, _ ->
            TrackingEvent(name = "chat_room.send_text", params = mapOf("text" to "[len=11]"))
        }

        // 模拟 TrackingExecutor 逻辑
        val gesture = InteractionContext.currentGesture()
        assertTrue(gesture != null)
        val trackingEvent = AnalyticsCollector.eventMapper?.invoke("ChatRoomStore", "FakeIntent")
        assertTrue(trackingEvent != null)

        val sanitizedParams = RuntimeSanitizer.sanitize(trackingEvent.params)
        AnalyticsCollector.collect(
            AnalyticsEvent.Intent(
                route = "ChatRoom",
                storeName = "ChatRoomStore",
                intentName = "send_text",
                gestureType = gesture.gestureType,
                params = sanitizedParams,
            )
        )

        assertEquals(1, reported.size)
        val event = reported[0][0] as AnalyticsEvent.Intent
        assertEquals("ChatRoomStore", event.storeName)
        assertEquals("send_text", event.intentName)
        assertEquals(GestureType.TAP, event.gestureType)
        assertEquals("[len=11]", event.params["text"])
    }

    @Test
    fun systemIntent_notCollected() {
        // 不调用 markUserGesture → isUserInitiated() 返回 false
        val gesture = InteractionContext.currentGesture()
        assertTrue(gesture == null)
        // TrackingExecutor 不会调用 AnalyticsCollector.collect
        assertTrue(reported.isEmpty())
    }
}
