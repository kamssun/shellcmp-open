package com.example.archshowcase.presentation.theme

import androidx.compose.ui.text.TextStyle
import kotlin.test.Test
import kotlin.test.assertNotEquals

class AppTypographyTest {

    private val typography = AppTypography()

    @Test
    fun defaultInstanceHasNonDefaultHeadlineLarge() {
        assertNotEquals(TextStyle.Default, typography.headlineLarge)
    }

    @Test
    fun defaultInstanceHasNonDefaultTitleMedium() {
        assertNotEquals(TextStyle.Default, typography.titleMedium)
    }

    @Test
    fun defaultInstanceHasNonDefaultBodyMedium() {
        assertNotEquals(TextStyle.Default, typography.bodyMedium)
    }

    @Test
    fun defaultInstanceHasNonDefaultLabelMedium() {
        assertNotEquals(TextStyle.Default, typography.labelMedium)
    }

    @Test
    fun copyProducesNewInstance() {
        val copy = typography.copy(bodyMedium = TextStyle.Default)
        assertNotEquals(typography.bodyMedium, copy.bodyMedium)
    }
}
