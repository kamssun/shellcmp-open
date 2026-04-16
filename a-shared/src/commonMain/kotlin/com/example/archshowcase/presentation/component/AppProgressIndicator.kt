package com.example.archshowcase.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.presentation.theme.AppTheme

/** 替代 Material3 CircularProgressIndicator（不定进度）。在 AppButton 内部自动使用 contentColor。 */
@Composable
fun AppCircularProgress(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Float = 4f,
) {
    val resolvedColor = when {
        color != Color.Unspecified -> color
        LocalContentColor.current != Color.Unspecified -> LocalContentColor.current
        else -> AppTheme.colors.primary
    }
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Canvas(modifier = modifier.size(40.dp)) {
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        drawArc(
            color = resolvedColor,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** 替代 Material3 LinearProgressIndicator */
@Composable
fun AppLinearProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.primary,
    trackColor: Color = AppTheme.colors.surfaceVariant,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Track
        drawLine(
            color = trackColor,
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = height,
            cap = StrokeCap.Round,
        )

        // Progress
        val progressWidth = width * progress().coerceIn(0f, 1f)
        if (progressWidth > 0f) {
            drawLine(
                color = color,
                start = Offset(0f, height / 2),
                end = Offset(progressWidth, height / 2),
                strokeWidth = height,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview
@Composable
fun AppProgressPreview() = AppTheme {
    Column {
        AppCircularProgress()
        Spacer(Modifier.height(16.dp))
        AppLinearProgress(progress = { 0.6f }, modifier = Modifier.fillMaxWidth().height(4.dp))
    }
}
