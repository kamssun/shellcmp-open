package com.example.archshowcase.presentation.theme

import kotlin.test.Test
import kotlin.test.assertNotNull

class AppThemeDefaultsTest {

    @Test
    fun defaultColorSchemeIsAvailable() {
        assertNotNull(AppThemeDefaults.colors)
    }

    @Test
    fun defaultTypographyIsAvailable() {
        assertNotNull(AppThemeDefaults.typography)
    }

    @Test
    fun defaultShapesIsAvailable() {
        assertNotNull(AppThemeDefaults.shapes)
    }
}
