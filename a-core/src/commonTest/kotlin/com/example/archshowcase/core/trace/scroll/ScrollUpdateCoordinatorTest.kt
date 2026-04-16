package com.example.archshowcase.core.trace.scroll

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ScrollUpdateCoordinatorTest {

    @Test
    fun `initial state is not scrolling`() {
        val coordinator = ScrollUpdateCoordinator()
        assertFalse(coordinator.isUserScrolling())
    }

    @Test
    fun `isUserScrolling returns true during block execution`() {
        val coordinator = ScrollUpdateCoordinator()
        coordinator.runWithUserScroll {
            assertTrue(coordinator.isUserScrolling())
        }
    }

    @Test
    fun `isUserScrolling returns false after block completes`() {
        val coordinator = ScrollUpdateCoordinator()
        coordinator.runWithUserScroll { }
        assertFalse(coordinator.isUserScrolling())
    }

    @Test
    fun `flag resets even if block throws`() {
        val coordinator = ScrollUpdateCoordinator()
        try {
            coordinator.runWithUserScroll { throw RuntimeException("test") }
            fail("should have thrown")
        } catch (_: RuntimeException) { }
        assertFalse(coordinator.isUserScrolling())
    }
}
