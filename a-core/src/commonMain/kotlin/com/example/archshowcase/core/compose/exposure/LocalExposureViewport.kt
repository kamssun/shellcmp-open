package com.example.archshowcase.core.compose.exposure

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/** 曝光视口信息，由 ExposureLazyColumn 提供 */
data class ExposureViewport(
    val bounds: Rect = Rect.Zero,
    val listId: String = "",
)

/**
 * 向子组件提供视口 State 对象的 CompositionLocal。
 * 使用 staticCompositionLocalOf + State<> 包装：
 * - State 对象引用稳定，不触发重组
 * - 子组件在 onGloballyPositioned 中读取 .value，避免滚动时全 item 重组
 */
val LocalExposureViewport = staticCompositionLocalOf<State<ExposureViewport>> {
    mutableStateOf(ExposureViewport())
}
