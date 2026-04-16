package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.GestureType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InteractionContextBoundaryTest {

    @BeforeTest
    fun setUp() {
        InteractionContext.reset()
    }

    @AfterTest
    fun tearDown() {
        InteractionContext.reset()
    }

    @Test
    fun syncMultipleIntents_allWithinCallStack() {
        withUserGesture("AppButton") {
            assertTrue(InteractionContext.isUserInitiated())
            assertTrue(InteractionContext.isUserInitiated())
        }
        assertFalse(InteractionContext.isUserInitiated())
    }

    @Test
    fun noAppComponent_clickable_notUserInitiated() {
        assertFalse(InteractionContext.isUserInitiated())
    }

    @Test
    fun differentGestureTypes_preserved() {
        withUserGesture("AppSwitch", GestureType.SWITCH) {
            val gesture = InteractionContext.currentGesture()
            assertNotNull(gesture)
            assertEquals(GestureType.SWITCH, gesture.gestureType)
            assertEquals("AppSwitch", gesture.component)
        }
    }

    @Test
    fun afterCallStackEnds_gestureCleared() {
        withUserGesture("AppButton") {
            assertTrue(InteractionContext.isUserInitiated())
        }
        assertFalse(InteractionContext.isUserInitiated())
    }

    @Test
    fun depthNeverGoesNegative() {
        InteractionContext.endUserGesture()
        InteractionContext.endUserGesture()
        assertFalse(InteractionContext.isUserInitiated())
        // subsequent mark still works
        InteractionContext.markUserGesture("AppButton")
        assertTrue(InteractionContext.isUserInitiated())
    }
}
