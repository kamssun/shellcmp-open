package com.example.archshowcase.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.core.analytics.appClickable
import com.example.archshowcase.core.analytics.model.GestureType
import com.example.archshowcase.presentation.theme.AppTheme

/** 替代 Material3 FilterChip */
@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (selected) AppTheme.colors.primary.copy(alpha = 0.12f) else AppTheme.colors.surface
    val borderColor = if (selected) AppTheme.colors.primary else AppTheme.colors.outline

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .appClickable(
                component = "AppFilterChip",
                gestureType = GestureType.CHIP,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        label()
    }
}

@Preview
@Composable
fun AppFilterChipPreview() = AppTheme {
    AppFilterChip(selected = true, onClick = {}, label = { AppText("Filter") })
}
