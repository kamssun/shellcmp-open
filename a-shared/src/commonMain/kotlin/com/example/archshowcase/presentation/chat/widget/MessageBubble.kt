package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import com.example.archshowcase.core.analytics.appClickable
import com.example.archshowcase.core.analytics.appCombinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.presentation.common.AsyncImage
import com.example.archshowcase.presentation.component.AppText
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_recall_other
import com.example.archshowcase.resources.chat_recall_self
import com.example.archshowcase.resources.chat_unsupported_type

@Composable
fun MessageBubble(
    message: ChatMessage,
    onLongClick: () -> Unit,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.isRecalled) {
        RecalledMessage(message)
        return
    }

    val isMine = message.isMine
    val arrangement = if (isMine) Arrangement.End else Arrangement.Start

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMine) {
            // Avatar
            AsyncImage(
                model = message.senderAvatar,
                contentDescription = message.senderName,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
        }

        // Failed indicator (left of bubble for mine)
        if (isMine && message.status == SendStatus.FAILED) {
            FailedIndicator(onClick = onResendClick)
            Spacer(Modifier.width(4.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            if (!isMine) {
                AppText(
                    text = message.senderName,
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            BubbleContent(
                body = message.body,
                isMine = isMine,
                onLongClick = onLongClick
            )
        }

        // Failed indicator (right of bubble for others — shouldn't happen but safe)
        if (!isMine && message.status == SendStatus.FAILED) {
            Spacer(Modifier.width(4.dp))
            FailedIndicator(onClick = onResendClick)
        }
    }
}

@Composable
private fun BubbleContent(
    body: MessageBody,
    isMine: Boolean,
    onLongClick: () -> Unit
) {
    val bubbleColor = if (isMine) Color(0xFF95EC69) else AppTheme.colors.surface
    val textColor = if (isMine) Color(0xFF1C1C1C) else AppTheme.colors.onSurface
    val shape = RoundedCornerShape(
        topStart = if (isMine) 12.dp else 4.dp,
        topEnd = if (isMine) 4.dp else 12.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )

    when (body) {
        is MessageBody.Text -> {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .appCombinedClickable(component = "MessageBubble", onLongClick = onLongClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AppText(
                    text = body.text,
                    style = AppTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }

        is MessageBody.Image -> {
            val maxW = 180.dp
            val maxH = 240.dp
            val ratio = (body.width.toFloat() / body.height.coerceAtLeast(1).toFloat())
                .coerceIn(0.3f, 3f)
            val heightFromWidth = maxW / ratio
            val displayWidth = if (heightFromWidth <= maxH) maxW else maxH * ratio
            val displayHeight = if (heightFromWidth <= maxH) heightFromWidth else maxH
            AsyncImage(
                model = body.url,
                contentDescription = if (body.isGif) "GIF" else "Image",
                modifier = Modifier
                    .width(displayWidth)
                    .height(displayHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .appCombinedClickable(component = "MessageBubble", onLongClick = onLongClick)
            )
        }

        is MessageBody.Sticker -> {
            AsyncImage(
                model = body.url,
                contentDescription = "Sticker",
                modifier = Modifier
                    .size(100.dp)
                    .appCombinedClickable(component = "MessageBubble", onLongClick = onLongClick)
            )
        }

        is MessageBody.Voice -> {
            val seconds = body.durationMs / 1000
            val width = (60 + seconds * 2).coerceAtMost(180).dp
            Box(
                modifier = Modifier
                    .width(width)
                    .clip(shape)
                    .background(bubbleColor)
                    .appCombinedClickable(component = "MessageBubble", onLongClick = onLongClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AppText(
                    text = "\uD83C\uDF99 ${seconds}″",
                    style = AppTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }

        is MessageBody.Video -> {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .appCombinedClickable(component = "MessageBubble", onLongClick = onLongClick),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = body.thumbnailUrl,
                    contentDescription = "Video",
                    modifier = Modifier.fillMaxSize()
                )
                AppText(
                    text = "▶",
                    color = Color.White,
                    style = AppTheme.typography.bodyLarge,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .wrapContentSize(Alignment.Center)
                )
            }
        }

        is MessageBody.System -> {
            // Handled separately, not in bubble
        }

        is MessageBody.Gift, is MessageBody.Broadcast, is MessageBody.Unknown -> {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AppText(
                    text = tr(Res.string.chat_unsupported_type),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecalledMessage(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = if (message.isMine) tr(Res.string.chat_recall_self) else tr(Res.string.chat_recall_other, message.senderName),
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun FailedIndicator(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.error)
            .appClickable(component = "FailedIndicator", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppText(text = "!", color = Color.White, style = AppTheme.typography.labelSmall)
    }
}

@Preview
@Composable
private fun MessageBubblePreview() {
    AppTheme {
        MessageBubble(
            message = ChatMessage(
                id = "preview_1",
                conversationId = "conv_1",
                senderId = "user_1",
                senderName = "张三",
                senderAvatar = null,
                body = MessageBody.Text("你好，这是一条预览消息"),
                timestamp = 0L,
                isMine = false,
                status = SendStatus.SENT
            ),
            onLongClick = {},
            onResendClick = {}
        )
    }
}
