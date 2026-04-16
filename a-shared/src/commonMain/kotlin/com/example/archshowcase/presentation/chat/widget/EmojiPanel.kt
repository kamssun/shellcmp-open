package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.archshowcase.core.analytics.appClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row as LayoutRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.common.AsyncImage
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_tab_emoji
import com.example.archshowcase.resources.chat_tab_sticker
import kotlinx.coroutines.launch

private val commonEmojis = listOf(
    "😊", "😂", "🤣", "❤️", "😍", "🙏", "😘", "🥰", "😁", "👍",
    "🔥", "😭", "🥺", "😅", "🤔", "😎", "😢", "💪", "🎉", "😳",
    "🤗", "🫡", "👋", "😏", "💀", "😤", "🥳", "🫠", "😱", "🤡",
    "💯", "✨", "👀", "🙈", "💕", "😡", "🫣", "😴", "🤝", "🫶"
)

private val stickerUrls = (0 until 20).map { i ->
    Pair("sticker_$i", "https://picsum.photos/seed/sticker_panel_$i/120/120")
}

@Composable
fun EmojiPanel(
    onEmojiClick: (String) -> Unit,
    onStickerClick: (stickerId: String, url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(AppTheme.colors.surface)
    ) {
        // Tab bar
        TabRow(
            selectedIndex = pagerState.currentPage,
            onTabClick = { scope.launch { pagerState.animateScrollToPage(it) } }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> EmojiGrid(onEmojiClick = onEmojiClick)
                1 -> StickerGrid(onStickerClick = onStickerClick)
            }
        }
    }
}

@Composable
private fun TabRow(selectedIndex: Int, onTabClick: (Int) -> Unit) {
    val tabs = listOf(tr(Res.string.chat_tab_emoji), tr(Res.string.chat_tab_sticker))
    LayoutRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            AppText(
                text = title,
                style = AppTheme.typography.labelMedium,
                color = if (selected) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onTabClick(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun EmojiGrid(onEmojiClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        items(
            items = commonEmojis,
            key = { it },
            contentType = { "emoji" }
        ) { emoji ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .appClickable(component = "EmojiGrid", onClick = { onEmojiClick(emoji) }),
                contentAlignment = Alignment.Center
            ) {
                AppText(text = emoji, style = AppTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun StickerGrid(onStickerClick: (String, String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        items(
            items = stickerUrls,
            key = { it.first },
            contentType = { "sticker" }
        ) { (id, url) ->
            AsyncImage(
                model = url,
                contentDescription = "Sticker",
                modifier = Modifier
                    .size(70.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .appClickable(component = "StickerGrid", onClick = { onStickerClick(id, url) })
            )
        }
    }
}

@Preview
@Composable
private fun EmojiPanelPreview() {
    AppTheme {
        EmojiPanel(onEmojiClick = {}, onStickerClick = { _, _ -> })
    }
}
