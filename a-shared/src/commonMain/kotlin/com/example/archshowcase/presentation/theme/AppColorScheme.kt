package com.example.archshowcase.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** 应用语义颜色体系，初始值映射自 Material3 默认 light 色值 */
@Immutable
data class AppColorScheme(
    val primary: Color = Color(0xFF6750A4),
    val onPrimary: Color = Color(0xFFFFFFFF),
    val primaryContainer: Color = Color(0xFFEADDFF),
    val onPrimaryContainer: Color = Color(0xFF21005D),
    val background: Color = Color(0xFFFFFBFE),
    val surface: Color = Color(0xFFFFFBFE),
    val surfaceVariant: Color = Color(0xFFE7E0EC),
    val onSurface: Color = Color(0xFF1C1B1F),
    val onSurfaceVariant: Color = Color(0xFF49454F),
    val error: Color = Color(0xFFB3261E),
    val errorContainer: Color = Color(0xFFF9DEDC),
    val onError: Color = Color(0xFFFFFFFF),
    val onErrorContainer: Color = Color(0xFF410E0B),
    val outline: Color = Color(0xFF79747E),
    val tertiaryContainer: Color = Color(0xFFFFD8E4),
    val onTertiaryContainer: Color = Color(0xFF31111D),
)
