package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.GestureType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InteractionContextTest {

    @BeforeTest
    fun setUp() {
        InteractionContext.reset()
    }

    @AfterTest
    fun tearDown() {
        InteractionContext.reset()
    }

    @Test
    fun markUserGesture_thenIsUserInitiated_returnsTrue() {
        InteractionContext.markUserGesture("AppButton")
        assertTrue(InteractionContext.isUserInitiated())
    }

    @Test
    fun withoutMarkUserGesture_isUserInitiated_returnsFalse() {
        assertFalse(InteractionContext.isUserInitiated())
    }

    @Test
    fun currentGesture_returnsCorrectInfo() {
        InteractionContext.markUserGesture("AppSwitch", GestureType.SWITCH)
        val gesture = InteractionContext.currentGesture()
        assertNotNull(gesture)
        assertEquals("AppSwitch", gesture.component)
        assertEquals(GestureType.SWITCH, gesture.gestureType)
    }

    @Test
    fun consecutiveMarkUserGesture_overridesPrevious() {
        InteractionContext.markUserGesture("AppButton")
        InteractionContext.markUserGesture("AppSwitch", GestureType.SWITCH)
        val gesture = InteractionContext.currentGesture()
        assertNotNull(gesture)
        assertEquals("AppSwitch", gesture.component)
        assertEquals(GestureType.SWITCH, gesture.gestureType)
    }

    @Test
    fun endUserGesture_clearsContext() {
        InteractionContext.markUserGesture("AppButton")
        assertTrue(InteractionContext.isUserInitiated())
        InteractionContext.endUserGesture()
        assertFalse(InteractionContext.isUserInitiated())
        assertNull(InteractionContext.currentGesture())
    }

    @Test
    fun reset_clearsGestureInfo() {
        InteractionContext.markUserGesture("AppButton")
        InteractionContext.reset()
        assertFalse(InteractionContext.isUserInitiated())
        assertNull(InteractionContext.currentGesture())
    }

    @Test
    fun defaultGestureType_isTap() {
        InteractionContext.markUserGesture("AppButton")
        val gesture = InteractionContext.currentGesture()
        assertNotNull(gesture)
        assertEquals(GestureType.TAP, gesture.gestureType)
    }

    @Test
    fun nestedWithUserGesture_depthCountCorrect() {
        withUserGesture("Outer") {
            assertTrue(InteractionContext.isUserInitiated())
            withUserGesture("Inner") {
                assertTrue(InteractionContext.isUserInitiated())
                assertEquals("Inner", InteractionContext.currentGesture()?.component)
            }
            // outer still active
            assertTrue(InteractionContext.isUserInitiated())
        }
        // all ended
        assertFalse(InteractionContext.isUserInitiated())
    }

    @Test
    fun withUserGesture_exceptionStillClearsContext() {
        try {
            withUserGesture("AppButton") {
                assertTrue(InteractionContext.isUserInitiated())
                throw RuntimeException("test")
            }
        } catch (_: RuntimeException) {
            // expected
        }
        assertFalse(InteractionContext.isUserInitiated())
        assertNull(InteractionContext.currentGesture())
    }

    @Test
    fun withUserGesture_setsAndClearsContext() {
        assertFalse(InteractionContext.isUserInitiated())
        withUserGesture("AppButton") {
            assertTrue(InteractionContext.isUserInitiated())
            assertEquals("AppButton", InteractionContext.currentGesture()?.component)
        }
        assertFalse(InteractionContext.isUserInitiated())
    }

    @Test
    fun withUserGesture_customGestureType() {
        withUserGesture("AppSwitch", GestureType.SWITCH) {
            val gesture = InteractionContext.currentGesture()
            assertNotNull(gesture)
            assertEquals(GestureType.SWITCH, gesture.gestureType)
        }
    }
}
