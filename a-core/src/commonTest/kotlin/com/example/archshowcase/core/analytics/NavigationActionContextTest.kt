package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.NavigationAction
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationActionContextTest {

    @AfterTest
    fun reset() {
        NavigationActionContext.current = NavigationAction.UNKNOWN
    }

    @Test
    fun defaultValue_isUnknown() {
        assertEquals(NavigationAction.UNKNOWN, NavigationActionContext.current)
    }

    @Test
    fun set_andReadBack() {
        NavigationActionContext.current = NavigationAction.PUSH
        assertEquals(NavigationAction.PUSH, NavigationActionContext.current)
    }

    @Test
    fun resetToUnknown_works() {
        NavigationActionContext.current = NavigationAction.REPLACE_ALL
        NavigationActionContext.current = NavigationAction.UNKNOWN
        assertEquals(NavigationAction.UNKNOWN, NavigationActionContext.current)
    }
}
