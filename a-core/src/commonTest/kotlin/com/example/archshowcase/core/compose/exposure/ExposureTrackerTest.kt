package com.example.archshowcase.core.compose.exposure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExposureTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val events = mutableListOf<ExposureEvent>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        events.clear()
        ExposureTracker.reset()
        ExposureTracker.dispatcher = testDispatcher
    }

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
    }

    @AfterTest
    fun tearDown() {
        ExposureTracker.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun visibleAboveThreshold_afterDwell_triggersExposure() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertEquals(1, events.size)
        assertEquals("AppButton", events[0].componentType)
        assertEquals("btn_1", events[0].exposureKey)
    }

    @Test
    fun visibleBelowThreshold_noExposure() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.3f)
        advance(1000L)
        assertTrue(events.isEmpty())
    }

    @Test
    fun visibleThenHiddenBeforeDwell_noExposure() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(300L)
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.2f) // 滑出
        advance(500L)
        assertTrue(events.isEmpty())
    }

    @Test
    fun duplicateExposure_deduped() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertEquals(1, events.size)

        // 第二次满足条件
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.8f)
        advance(501L)
        assertEquals(1, events.size) // 不重复
    }

    @Test
    fun resetPage_clearsExposedKeys() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertEquals(1, events.size)

        ExposureTracker.resetPage()

        // 重新曝光
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertEquals(2, events.size)
    }

    @Test
    fun pauseAll_preventsExposure() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(200L)
        ExposureTracker.pauseAll()
        advance(500L)
        assertTrue(events.isEmpty())
    }

    @Test
    fun resumeAll_afterPause_allowsNewExposure() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.pauseAll()
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertTrue(events.isEmpty())

        ExposureTracker.resumeAll()
        ExposureTracker.reportVisibility("btn_2", "AppButton", 0.6f)
        advance(501L)
        assertEquals(1, events.size)
    }

    @Test
    fun exposureEvent_containsListId() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f, listId = "chat_list")
        advance(501L)
        assertEquals("chat_list", events[0].listId)
    }

    @Test
    fun exposureEvent_containsParams() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        val params = mapOf("image_id" to "123", "position" to "0")
        ExposureTracker.reportVisibility(
            "image_123", "ImageCard", 0.6f,
            listId = "image_list", params = params,
        )
        advance(501L)
        assertEquals(1, events.size)
        assertEquals("ImageCard", events[0].componentType)
        assertEquals("image_123", events[0].exposureKey)
        assertEquals(params, events[0].params)
    }

    @Test
    fun pauseResume_sameKey_canReExpose() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }

        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(200L)
        ExposureTracker.pauseAll()
        advance(500L)
        assertTrue(events.isEmpty())

        ExposureTracker.resumeAll()
        // 同一 key 重新开始
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertEquals(1, events.size)
        assertEquals("btn_1", events[0].exposureKey)
    }

    @Test
    fun exposureEvent_emptyParamsByDefault() = runTest(testDispatcher) {
        ExposureTracker.init(this, ExposureConfig(dwellTimeMs = 500L))
        ExposureTracker.onExposure = { events.add(it) }
        ExposureTracker.reportVisibility("btn_1", "AppButton", 0.6f)
        advance(501L)
        assertEquals(emptyMap(), events[0].params)
    }
}
