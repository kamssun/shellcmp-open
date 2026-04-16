package com.example.archshowcase.core.trace.scroll

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import com.example.archshowcase.core.compose.OBOLaunchedEffect
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

/**
 * Compose 侧滚动恢复 Effect
 *
 * 监听 scrollRestoreEvent，在列表准备好后恢复滚动位置。
 * 用于 TimeTravel 回放场景。
 *
 * @param minItemCount 最小 item 数量，列表至少有这么多数据才尝试恢复。默认 1。
 */
@Composable
fun ScrollRestoreEffect(
    listState: LazyListState,
    scrollRestoreEvent: SharedFlow<ScrollPosition>,
    minItemCount: Int = 1
) {
    OBOLaunchedEffect(Unit) {
        scrollRestoreEvent.collect { position ->
            val requiredCount = maxOf(minItemCount, position.firstVisibleIndex + 1)
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it >= requiredCount }
            listState.scrollToItem(position.firstVisibleIndex, position.offset)
        }
    }
}
