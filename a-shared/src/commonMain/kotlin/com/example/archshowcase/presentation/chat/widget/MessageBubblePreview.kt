package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme

@Preview(name = "Message Bubble - Media", widthDp = 360, heightDp = 680)
@Composable
private fun MessageBubbleMediaPreview() {
    PreviewWrapper { _ ->
        MessageBubblePreviewList(
            messages = listOf(
                previewMessage(
                    id = "preview_image",
                    body = MessageBody.Image(
                        url = "https://picsum.photos/seed/message_preview_image/640/480",
                        width = 640,
                        height = 480
                    )
                ),
                previewMessage(
                    id = "preview_gif",
                    body = MessageBody.Image(
                        url = "https://picsum.photos/seed/message_preview_gif/240/240",
                        width = 240,
                        height = 240,
                        isGif = true
                    ),
                    isMine = true
                ),
                previewMessage(
                    id = "preview_sticker",
                    body = MessageBody.Sticker(
                        stickerId = "preview_sticker",
                        url = "https://picsum.photos/seed/message_preview_sticker/120/120"
                    )
                ),
                previewMessage(
                    id = "preview_video",
                    body = MessageBody.Video(
                        url = "mock://video/preview",
                        thumbnailUrl = "https://picsum.photos/seed/message_preview_video/320/180",
                        durationMs = 30_000
                    ),
                    isMine = true
                )
            )
        )
    }
}

@Preview(name = "Message Bubble - States", widthDp = 360, heightDp = 300)
@Composable
private fun MessageBubbleStatesPreview() {
    PreviewWrapper { _ ->
        MessageBubblePreviewList(
            messages = listOf(
                previewMessage(
                    id = "preview_voice",
                    body = MessageBody.Voice(
                        url = "mock://voice/preview",
                        durationMs = 18_000
                    )
                ),
                previewMessage(
                    id = "preview_unsupported",
                    body = MessageBody.Gift(
                        giftId = "preview_gift",
                        name = "礼物",
                        count = 1
                    ),
                    isMine = true
                ),
                previewMessage(
                    id = "preview_recalled_other",
                    body = MessageBody.Text("已撤回"),
                    isRecalled = true
                ),
                previewMessage(
                    id = "preview_recalled_mine",
                    body = MessageBody.Text("已撤回"),
                    isMine = true,
                    isRecalled = true
                )
            )
        )
    }
}

@Composable
private fun MessageBubblePreviewList(messages: List<ChatMessage>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        messages.forEach { message ->
            MessageBubble(
                message = message,
                onLongClick = {},
                onResendClick = {}
            )
        }
    }
}

private fun previewMessage(
    id: String,
    body: MessageBody,
    isMine: Boolean = false,
    status: SendStatus = SendStatus.SENT,
    isRecalled: Boolean = false
): ChatMessage =
    ChatMessage(
        id = id,
        conversationId = "preview_conv",
        senderId = if (isMine) "local_user" else "user_1",
        senderName = if (isMine) "我" else "张三",
        senderAvatar = null,
        body = body,
        timestamp = 0L,
        isMine = isMine,
        status = status,
        isRecalled = isRecalled
    )
