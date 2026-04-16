package com.example.archshowcase.presentation.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 当前内容区域的前景色，供 AppText 在 AppButton 内部使用 */
val LocalContentColor = staticCompositionLocalOf { Color.Unspecified }
