package com.example.archshowcase.presentation.demo.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.core.compose.OBOLaunchedEffect
import com.example.archshowcase.core.compose.exposure.ExposureLazyColumn
import com.example.archshowcase.core.trace.scroll.ScrollRestoreEffect
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.presentation.common.AsyncImage
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCircularProgress
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.btn_back
import com.example.archshowcase.resources.btn_retry
import com.example.archshowcase.resources.text_no_more
import com.example.archshowcase.resources.title_image_list
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(FlowPreview::class)
@Composable
fun ImageDemoContent(component: ImageDemoComponent) {
    val state = component.state.rememberFields()
    val listState = rememberLazyListState()

    OBOLaunchedEffect(Unit) {
        if (component.state.value.images.isEmpty()) {
            component.loadInitial()
        }
    }

    ScrollRestoreEffect(
        listState = listState,
        scrollRestoreEvent = component.scrollRestoreEvent
    )

    OBOLaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .debounce(100)
            .distinctUntilChanged()
            .collect { (first, offset) ->
                component.updateScrollPosition(first, offset)
            }
    }

    val readyToLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 5 && !state.isLoadingMore && state.hasMore
        }
    }

    OBOLaunchedEffect(Unit) {
        snapshotFlow { readyToLoadMore }
            .distinctUntilChanged()
            .filter { it }
            .collect { component.loadMore() }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = {
                    AppText(tr(Res.string.title_image_list, state.images.size, state.totalCount), style = AppTheme.typography.titleMedium)
                },
                navigationIcon = {
                    AppTextButton(onClick = { component.onBack() }) {
                        AppText(tr(Res.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.images.isEmpty() && state.error == null -> {
                    AppCircularProgress(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.isEmpty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppText(state.error!!)
                        Spacer(modifier = Modifier.height(16.dp))
                        AppButton(onClick = { component.loadInitial() }) {
                            AppText(tr(Res.string.btn_retry))
                        }
                    }
                }
                else -> {
                    ExposureLazyColumn(
                        listId = "image_list",
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.images,
                            key = { it.id },
                            contentType = { "image_card" }
                        ) { image ->
                            ImageCard(image = image)
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_more", contentType = "loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppCircularProgress(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }

                        if (!state.hasMore && state.images.isNotEmpty()) {
                            item(key = "no_more", contentType = "footer") {
                                AppText(
                                    text = tr(Res.string.text_no_more),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    style = AppTheme.typography.bodySmall,
                                    color = AppTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCard(image: ImageItem) {
    val shape = remember { RoundedCornerShape(12.dp) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surfaceVariant, shape)
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = image.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )
        AppText(
            text = image.title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview
@Composable
fun ImageDemoContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultImageDemoComponent(componentContext) }
    ImageDemoContent(component)
}
