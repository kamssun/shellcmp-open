package com.example.archshowcase.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** 默认主题实例，可在非 Compose 环境（测试等）中使用 */
object AppThemeDefaults {
    val colors: AppColorScheme = AppColorScheme()
    val typography: AppTypography = AppTypography()
    val shapes: AppShapes = AppShapes()
}

internal val LocalAppColors = staticCompositionLocalOf { AppThemeDefaults.colors }
internal val LocalAppTypography = staticCompositionLocalOf { AppThemeDefaults.typography }
internal val LocalAppShapes = staticCompositionLocalOf { AppThemeDefaults.shapes }

/** 应用根主题，替代 MaterialTheme */
@Composable
fun AppTheme(
    colors: AppColorScheme = AppThemeDefaults.colors,
    typography: AppTypography = AppThemeDefaults.typography,
    shapes: AppShapes = AppThemeDefaults.shapes,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppShapes provides shapes,
        content = content,
    )
}

/** 主题访问器 */
object AppTheme {
    val colors: AppColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable @ReadOnlyComposable
        get() = LocalAppShapes.current
}
