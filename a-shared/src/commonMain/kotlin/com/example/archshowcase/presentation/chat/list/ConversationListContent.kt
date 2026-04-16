@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.presentation.chat.list

import androidx.compose.foundation.background
import com.example.archshowcase.core.analytics.appClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.archshowcase.core.compose.exposure.ExposureLazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import com.example.archshowcase.core.compose.OBOLaunchedEffect
import com.example.archshowcase.core.trace.scroll.ScrollRestoreEffect
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.chat.mapper.BodyTypeKey
import com.example.archshowcase.chat.model.Conversation
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.chat.widget.GroupAvatarComposer
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_preview_broadcast
import com.example.archshowcase.resources.chat_preview_gif
import com.example.archshowcase.resources.chat_preview_gift
import com.example.archshowcase.resources.chat_preview_image
import com.example.archshowcase.resources.chat_preview_sticker
import com.example.archshowcase.resources.chat_preview_unknown
import com.example.archshowcase.resources.chat_preview_video
import com.example.archshowcase.resources.chat_preview_voice
import com.example.archshowcase.resources.chat_typing
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(FlowPreview::class)
@Composable
fun ConversationListContent(
    component: ConversationListComponent,
    modifier: Modifier = Modifier
) {
    val state = component.state.rememberFields()
    val previewStrings = rememberPreviewStrings()
    val listState = rememberLazyListState()

    ScrollRestoreEffect(
        listState = listState,
        scrollRestoreEvent = component.scrollRestoreEvent
    )

    OBOLaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .sample(100)
            .collect { (first, offset) ->
                component.updateScrollPosition(first, offset)
            }
    }

    OBOLaunchedEffect(Unit) {
        component.scrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ExposureLazyColumn(
            listId = "conversation_list",
            state = listState,
            modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)
        ) {
            items(
                items = state.conversations,
                key = { it.id },
                contentType = { "conversation" }
            ) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    previewStrings = previewStrings,
                    onClick = { component.onConversationClick(conversation.id) }
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    previewStrings: PreviewStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appClickable(component = "ConversationItem", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupAvatarComposer(
            avatars = conversation.memberAvatars,
            size = 48.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = conversation.name,
                    style = AppTheme.typography.bodyLarge,
                    color = AppTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                val timeText = remember(conversation.lastActiveTime) { formatTime(conversation.lastActiveTime) }
                AppText(
                    text = timeText,
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val messagePreview = formatDenormalizedPreview(
                    conversation.lastMsgBodyType,
                    conversation.lastMsgPreview,
                    previewStrings
                )
                val preview = if (conversation.isTyping && conversation.typingUserName != null) {
                    tr(Res.string.chat_typing, conversation.typingUserName!!)
                } else {
                    messagePreview
                }

                AppText(
                    text = preview,
                    style = AppTheme.typography.bodyMedium,
                    color = if (conversation.isTyping) AppTheme.colors.primary
                    else AppTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    UnreadBadge()
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.error)
    )
}

private data class PreviewStrings(
    val image: String, val gif: String, val sticker: String,
    val video: String, val unknown: String
)

@Composable
private fun rememberPreviewStrings() = PreviewStrings(
    image = tr(Res.string.chat_preview_image),
    gif = tr(Res.string.chat_preview_gif),
    sticker = tr(Res.string.chat_preview_sticker),
    video = tr(Res.string.chat_preview_video),
    unknown = tr(Res.string.chat_preview_unknown)
)

/** 从反规范化字段渲染列表预览文本 */
@Composable
private fun formatDenormalizedPreview(
    bodyType: String?,
    preview: String?,
    s: PreviewStrings
): String = when (bodyType) {
    BodyTypeKey.TEXT -> preview ?: ""
    BodyTypeKey.IMAGE -> s.image
    BodyTypeKey.IMAGE_GIF -> s.gif
    BodyTypeKey.STICKER -> s.sticker
    BodyTypeKey.VOICE -> tr(Res.string.chat_preview_voice, preview?.toLongOrNull() ?: 0)
    BodyTypeKey.VIDEO -> s.video
    BodyTypeKey.GIFT -> tr(Res.string.chat_preview_gift, preview ?: "")
    BodyTypeKey.BROADCAST -> tr(Res.string.chat_preview_broadcast, preview ?: "")
    BodyTypeKey.SYSTEM -> preview ?: ""
    BodyTypeKey.UNKNOWN -> s.unknown
    else -> ""
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    return if (local.date == now.date) {
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } else {
        "${local.monthNumber}/${local.dayOfMonth}"
    }
}

@Preview
@Composable
fun ConversationListContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultConversationListComponent(componentContext) }
    ConversationListContent(component)
}
