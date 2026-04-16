package com.example.archshowcase.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertNotEquals

class AppColorSchemeTest {

    private val colors = AppColorScheme()

    @Test
    fun defaultInstanceHasNonTransparentPrimary() {
        assertNotEquals(Color.Transparent, colors.primary)
    }

    @Test
    fun defaultInstanceHasNonTransparentBackground() {
        assertNotEquals(Color.Transparent, colors.background)
    }

    @Test
    fun defaultInstanceHasNonTransparentSurface() {
        assertNotEquals(Color.Transparent, colors.surface)
    }

    @Test
    fun defaultInstanceHasNonTransparentOnSurface() {
        assertNotEquals(Color.Transparent, colors.onSurface)
    }

    @Test
    fun defaultInstanceHasNonTransparentError() {
        assertNotEquals(Color.Transparent, colors.error)
    }

    @Test
    fun copyProducesNewInstance() {
        val copy = colors.copy(primary = Color.Red)
        assertNotEquals(colors.primary, copy.primary)
    }
}
