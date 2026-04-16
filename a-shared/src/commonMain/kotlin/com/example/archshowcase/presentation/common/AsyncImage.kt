package com.example.archshowcase.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.example.archshowcase.presentation.theme.AppTheme
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.compose.AsyncImage as CoilAsyncImage

/**
 * 项目级 AsyncImage 包装，提供默认 placeholder 和 error 状态颜色。
 * Precision.INEXACT 允许 Coil 根据 Composable 实际大小降采样，避免大图全量解码。
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = ColorPainter(AppTheme.colors.surfaceVariant),
    error: Painter? = ColorPainter(AppTheme.colors.errorContainer),
) {
    val context = LocalPlatformContext.current
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .precision(Precision.INEXACT)
            .build()
    }
    CoilAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
    )
}
