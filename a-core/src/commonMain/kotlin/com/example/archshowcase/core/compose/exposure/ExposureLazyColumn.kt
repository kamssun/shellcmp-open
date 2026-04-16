package com.example.archshowcase.core.compose.exposure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/**
 * LazyColumn 的薄包装：
 * 1. 自动向子组件提供 LocalExposureViewport
 * 2. 自动为所有带 key + contentType 的 item 注入曝光追踪
 * 3. 通过 ExposureTracker.paramsExtractor 自动提取 item 业务参数（KSP 生成）
 *
 * content 接收者为 [ExposureLazyListScope]，其成员 items() 优先于 LazyListScope 扩展函数，
 * 从而保留 List<T> 引用用于自动参数提取。
 */
@Composable
fun ExposureLazyColumn(
    listId: String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: ExposureLazyListScope.() -> Unit,
) {
    val viewportState = remember { mutableStateOf(ExposureViewport()) }

    CompositionLocalProvider(LocalExposureViewport provides viewportState) {
        LazyColumn(
            modifier = modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                val newBounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height,
                )
                val current = viewportState.value
                if (current.bounds != newBounds || current.listId != listId) {
                    viewportState.value = ExposureViewport(bounds = newBounds, listId = listId)
                }
            },
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            userScrollEnabled = userScrollEnabled,
        ) {
            ExposureLazyListScope(this).content()
        }
    }
}

/**
 * LazyListScope 的包装。成员函数 [items] 优先于 Compose 的 LazyListScope.items 扩展函数，
 * 从而保留 List<T> 引用，在自动追踪时通过 [ExposureTracker.paramsExtractor] 提取业务参数。
 *
 * - 有 key + contentType → 自动注入 trackExposure（含 KSP 提取的 params）
 * - 缺 key 或缺 contentType → 跳过（loading/footer 等辅助项）
 */
class ExposureLazyListScope(
    private val delegate: LazyListScope,
) : LazyListScope by delegate {

    /** 成员函数，优先于 LazyListScope.items(List<T>) 扩展函数 */
    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        contentType: (item: T) -> Any? = { null },
        itemContent: @Composable LazyItemScope.(item: T) -> Unit,
    ) {
        delegate.items(
            count = items.size,
            key = if (key != null) { index: Int -> key(items[index]) } else null,
            contentType = { index: Int -> contentType(items[index]) },
        ) { index ->
            val item = items[index]
            val itemKey = key?.invoke(item)
            val itemType = contentType(item)
            if (itemKey != null && itemType != null) {
                val params = ExposureTracker.paramsExtractor?.invoke(item as Any) ?: emptyMap()
                Box(modifier = Modifier.trackExposure(
                    componentType = itemType.toString(),
                    exposureKey = "${itemType}:$itemKey",
                    params = params,
                )) {
                    itemContent(item)
                }
            } else {
                itemContent(item)
            }
        }
    }

    /** 单项（header/footer/loading 等辅助项）不自动注入曝光追踪 */
    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyItemScope.() -> Unit,
    ) {
        delegate.item(key, contentType, content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        if (key != null) {
            delegate.items(count, key, contentType) { index ->
                val itemKey = key(index)
                val itemType = contentType(index)
                if (itemType != null) {
                    Box(modifier = Modifier.trackExposure(
                        componentType = itemType.toString(),
                        exposureKey = "${itemType}:$itemKey",
                    )) {
                        itemContent(index)
                    }
                } else {
                    itemContent(index)
                }
            }
        } else {
            delegate.items(count, key, contentType, itemContent)
        }
    }
}
