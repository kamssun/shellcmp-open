package com.example.archshowcase.core.compose.exposure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * 为组件添加曝光追踪。由业务层在需要追踪曝光的元素上显式添加。
 *
 * @param componentType 组件类型标识（如 "ImageCard", "GiftCard"）
 * @param exposureKey 业务唯一标识（如 "image_${id}"），用于去重
 * @param params 业务上下文参数（如 imageId, position）
 */
@Composable
fun Modifier.trackExposure(
    componentType: String,
    exposureKey: String,
    params: Map<String, String> = emptyMap(),
): Modifier {
    val viewportState = LocalExposureViewport.current
    val currentParams = rememberUpdatedState(params)

    // 元素离开 composition 时取消待定的 dwell timer，防止幽灵曝光
    DisposableEffect(exposureKey) {
        onDispose { ExposureTracker.cancelPending(exposureKey) }
    }

    return this.onGloballyPositioned { coordinates ->
        val viewport = viewportState.value
        val visibleRatio = calculateVisibleRatio(coordinates, viewport.bounds)
        ExposureTracker.reportVisibility(
            exposureKey = exposureKey,
            componentType = componentType,
            visibleRatio = visibleRatio,
            listId = viewport.listId,
            params = currentParams.value,
        )
    }
}

internal fun calculateVisibleRatio(
    coordinates: LayoutCoordinates,
    viewportBounds: Rect,
): Float {
    if (!coordinates.isAttached) return 0f

    val position = coordinates.positionInRoot()
    val size = coordinates.size
    val totalArea = size.width.toFloat() * size.height.toFloat()
    if (totalArea <= 0f) return 0f

    val elementBounds = Rect(
        left = position.x,
        top = position.y,
        right = position.x + size.width,
        bottom = position.y + size.height,
    )

    // 无视口（非列表场景）则认为全部可见
    if (viewportBounds == Rect.Zero) return 1f

    val intersection = elementBounds.intersect(viewportBounds)
    if (intersection.isEmpty) return 0f

    val visibleArea = intersection.width * intersection.height
    return (visibleArea / totalArea).coerceIn(0f, 1f)
}
