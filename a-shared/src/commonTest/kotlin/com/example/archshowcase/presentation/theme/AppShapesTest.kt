package com.example.archshowcase.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.test.Test
import kotlin.test.assertIs

class AppShapesTest {

    private val shapes = AppShapes()

    @Test
    fun smallIsRoundedCornerShape() {
        assertIs<RoundedCornerShape>(shapes.small)
    }

    @Test
    fun mediumIsRoundedCornerShape() {
        assertIs<RoundedCornerShape>(shapes.medium)
    }

    @Test
    fun largeIsRoundedCornerShape() {
        assertIs<RoundedCornerShape>(shapes.large)
    }
}
