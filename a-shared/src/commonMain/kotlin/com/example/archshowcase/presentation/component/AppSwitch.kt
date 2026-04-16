package com.example.archshowcase.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.core.analytics.appClickable
import com.example.archshowcase.core.analytics.model.GestureType
import com.example.archshowcase.presentation.theme.AppTheme

/** 替代 Material3 Switch，Canvas 自绘 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
    )
    val trackColor = if (checked) AppTheme.colors.primary else AppTheme.colors.outline
    val thumbColor = if (checked) AppTheme.colors.onPrimary else AppTheme.colors.surface

    Canvas(
        modifier = modifier
            .requiredSize(width = 52.dp, height = 32.dp)
            .appClickable(
                component = "AppSwitch",
                gestureType = GestureType.SWITCH,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
    ) {
        val trackWidth = size.width
        val trackHeight = size.height
        val cornerRadius = CornerRadius(trackHeight / 2f)
        val alpha = if (enabled) 1f else 0.38f

        // Track
        drawRoundRect(
            color = trackColor.copy(alpha = alpha),
            size = Size(trackWidth, trackHeight),
            cornerRadius = cornerRadius,
        )

        // Thumb
        val thumbRadius = trackHeight * 0.4f
        val thumbPadding = (trackHeight - thumbRadius * 2) / 2
        val thumbMinX = thumbPadding + thumbRadius
        val thumbMaxX = trackWidth - thumbPadding - thumbRadius
        val thumbCx = thumbMinX + (thumbMaxX - thumbMinX) * thumbPosition

        drawCircle(
            color = thumbColor.copy(alpha = alpha),
            radius = thumbRadius,
            center = Offset(thumbCx, trackHeight / 2f),
        )
    }
}

@Preview
@Composable
fun AppSwitchPreview() = AppTheme {
    AppSwitch(checked = true, onCheckedChange = {})
}
