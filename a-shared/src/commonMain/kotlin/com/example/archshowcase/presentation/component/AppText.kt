package com.example.archshowcase.presentation.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.presentation.theme.AppTheme

/** 替代 Material3 Text，默认使用 AppTheme 样式。在 AppButton 内部自动继承 contentColor。 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val resolvedColor = when {
        color != Color.Unspecified -> color
        LocalContentColor.current != Color.Unspecified -> LocalContentColor.current
        else -> AppTheme.colors.onSurface
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolvedColor),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview
@Composable
fun AppTextPreview() = AppTheme {
    AppText("Hello, World!")
}
