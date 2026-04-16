package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.archshowcase.presentation.common.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.presentation.theme.AppTheme
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * WeChat-style group avatar: compose up to 9 member avatars in a grid.
 */
@Composable
fun GroupAvatarComposer(
    avatars: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    gap: Dp = 1.dp
) {
    val displayAvatars = avatars.take(9)
    val count = displayAvatars.size

    if (count == 0) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(AppTheme.colors.surfaceVariant)
        )
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(AppTheme.colors.surfaceVariant)
    ) {
        GridAvatarLayout(
            count = count,
            totalSize = size,
            gap = gap
        ) {
            displayAvatars.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun GridAvatarLayout(
    count: Int,
    totalSize: Dp,
    gap: Dp,
    content: @Composable () -> Unit
) {
    Layout(content = content) { measurables, constraints ->
        val cols = when {
            count <= 1 -> 1
            count <= 4 -> 2
            else -> 3
        }
        val rows = ceil(count.toFloat() / cols).toInt()
        val totalSizePx = totalSize.roundToPx()
        val gapPx = gap.roundToPx()
        val cellSize = (totalSizePx - gapPx * (cols - 1)) / cols

        val placeables = measurables.map { measurable ->
            measurable.measure(
                constraints.copy(
                    minWidth = cellSize,
                    maxWidth = cellSize,
                    minHeight = cellSize,
                    maxHeight = cellSize
                )
            )
        }

        val totalContentHeight = rows * cellSize + (rows - 1) * gapPx
        val yOffset = (totalSizePx - totalContentHeight) / 2

        // Handle last row centering when it has fewer items
        layout(totalSizePx, totalSizePx) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / cols
                val col = index % cols

                val itemsInRow = if (row == rows - 1) count - row * cols else cols
                val rowXOffset = if (itemsInRow < cols) {
                    (totalSizePx - itemsInRow * cellSize - (itemsInRow - 1) * gapPx) / 2
                } else 0

                val x = rowXOffset + col * (cellSize + gapPx)
                val y = yOffset + row * (cellSize + gapPx)
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Preview
@Composable
private fun GroupAvatarComposerPreview() {
    AppTheme {
        GroupAvatarComposer(
            avatars = listOf(
                "https://picsum.photos/seed/a1/100/100",
                "https://picsum.photos/seed/a2/100/100",
                "https://picsum.photos/seed/a3/100/100",
                "https://picsum.photos/seed/a4/100/100"
            ),
            size = 48.dp
        )
    }
}
