package com.example.archshowcase.presentation.demo.obo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.archshowcase.core.compose.OBOLaunchedEffect
import com.example.archshowcase.core.trace.scroll.ScrollRestoreEffect
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppFilterChip
import com.example.archshowcase.presentation.component.AppLinearProgress
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppSwitch
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock

private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(FlowPreview::class)
@Composable
fun OBODemoContent(component: OBODemoComponent) {
    val state = component.state.rememberFields()
    val listState = rememberLazyListState()

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
            .collect { (index, offset) ->
                component.updateScrollPosition(index, offset)
            }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_obo_demo), style = AppTheme.typography.titleMedium) },
                navigationIcon = {
                    AppTextButton(onClick = { component.onBackClicked() }) {
                        AppText(tr(Res.string.btn_back))
                    }
                },
                actions = {
                    FpsIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ControlPanel(
                useOBO = state.useOBO,
                effectsPerItem = state.effectsPerItem,
                blockTimeMs = state.blockTimeMs,
                onToggleOBO = { component.onToggleOBO(it) },
                onSetEffectsPerItem = { component.onSetEffectsPerItem(it) },
                onSetBlockTime = { component.onSetBlockTime(it) },
                onReload = { component.onReload() }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(count = 30, key = { "$it-${state.reloadTrigger}" }, contentType = { "stress_item" }) { index ->
                    StressItem(
                        index = index,
                        useOBO = state.useOBO,
                        effectCount = state.effectsPerItem,
                        blockTimeMs = state.blockTimeMs
                    )
                }
                item(contentType = "spacer") { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FpsIndicator() {
    var fps by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastTime by remember { mutableLongStateOf(currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                frameCount++
                val now = currentTimeMillis()
                if (now - lastTime >= 1000) {
                    fps = frameCount
                    frameCount = 0
                    lastTime = now
                }
            }
        }
    }

    val color by remember {
        derivedStateOf {
            when {
                fps >= 55 -> Color(0xFF4CAF50)
                fps >= 40 -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .drawBehind { drawCircle(color) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        AppText(
            text = "$fps",
            style = AppTheme.typography.titleMedium,
            color = color,
        )
    }
}

@Composable
private fun ControlPanel(
    useOBO: Boolean,
    effectsPerItem: Int,
    blockTimeMs: Int,
    onToggleOBO: (Boolean) -> Unit,
    onSetEffectsPerItem: (Int) -> Unit,
    onSetBlockTime: (Int) -> Unit,
    onReload: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppText(tr(Res.string.text_obo_scheduling), style = AppTheme.typography.titleMedium)
                    AppText(
                        if (useOBO) tr(Res.string.text_obo_sequential) else tr(Res.string.text_obo_concurrent),
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
                AppSwitch(checked = useOBO, onCheckedChange = onToggleOBO)
                Spacer(modifier = Modifier.width(12.dp))
                AppButton(onClick = onReload) { AppText(tr(Res.string.btn_reload)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    AppText(tr(Res.string.text_effect_per_item), style = AppTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(5, 10, 20).forEach { count ->
                            AppFilterChip(
                                selected = effectsPerItem == count,
                                onClick = { onSetEffectsPerItem(count) },
                                label = { AppText("$count") }
                            )
                        }
                    }
                }

                Column {
                    AppText(tr(Res.string.text_block_time), style = AppTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 3, 5).forEach { ms ->
                            AppFilterChip(
                                selected = blockTimeMs == ms,
                                onClick = { onSetBlockTime(ms) },
                                label = { AppText("${ms}ms") }
                            )
                        }
                    }
                }
            }

            val totalMs = 30 * effectsPerItem * blockTimeMs
            Spacer(modifier = Modifier.height(8.dp))
            AppText(
                tr(Res.string.text_obo_total_block, effectsPerItem, blockTimeMs, totalMs),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.outline
            )
        }
    }
}

private fun blockingWork(ms: Int) {
    val end = currentTimeMillis() + ms
    @Suppress("ControlFlowWithEmptyBody")
    while (currentTimeMillis() < end) { }
}

@Composable
private fun StressItem(index: Int, useOBO: Boolean, effectCount: Int, blockTimeMs: Int) {
    var completed by remember { mutableIntStateOf(0) }

    repeat(effectCount) { effectIndex ->
        if (useOBO) {
            OBOLaunchedEffect(index, effectIndex, effectCount, blockTimeMs) {
                blockingWork(blockTimeMs)
                completed++
            }
        } else {
            LaunchedEffect(index, effectIndex, effectCount, blockTimeMs) {
                blockingWork(blockTimeMs)
                completed++
            }
        }
    }

    val progress = completed.toFloat() / effectCount

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "#$index",
                style = AppTheme.typography.labelMedium,
                modifier = Modifier.width(36.dp)
            )
            AppLinearProgress(
                progress = { progress },
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppText(
                text = "$completed/$effectCount",
                style = AppTheme.typography.bodySmall,
                color = if (completed == effectCount)
                    AppTheme.colors.primary
                else
                    AppTheme.colors.outline
            )
        }
    }
}

@Preview
@Composable
private fun OBODemoContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultOBODemoComponent(componentContext) }
    OBODemoContent(component)
}
