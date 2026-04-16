package com.example.archshowcase.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.presentation.theme.AppTheme

/** 替代 Material3 Card */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.surface,
    shape: Shape = AppTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor),
    ) {
        content()
    }
}

@Preview
@Composable
fun AppCardPreview() = AppTheme {
    AppCard { AppText("Card content", modifier = Modifier.padding(16.dp)) }
}
