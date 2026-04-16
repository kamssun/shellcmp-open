@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import com.example.archshowcase.core.analytics.appClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.remember
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.SendStatus
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_action_copy
import com.example.archshowcase.resources.chat_action_delete
import com.example.archshowcase.resources.chat_action_recall
import kotlin.time.Clock

private const val RECALL_TIMEOUT_MS = 2 * 60 * 1000L

@Composable
fun MessageActionPopup(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRecall: () -> Unit,
    onDelete: () -> Unit
) {
    val canCopy = message.body is MessageBody.Text
    val canRecall = remember(message.id) {
        message.isMine && (Clock.System.now().toEpochMilliseconds() - message.timestamp) < RECALL_TIMEOUT_MS
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF4A4A4A))
                .padding(4.dp)
        ) {
            if (canCopy) {
                ActionItem(text = tr(Res.string.chat_action_copy), onClick = { onCopy(); onDismiss() })
            }
            if (canRecall) {
                ActionItem(text = tr(Res.string.chat_action_recall), onClick = { onRecall(); onDismiss() })
            }
            ActionItem(text = tr(Res.string.chat_action_delete), onClick = { onDelete(); onDismiss() })
        }
    }
}

@Composable
private fun ActionItem(text: String, onClick: () -> Unit) {
    AppText(
        text = text,
        style = AppTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier
            .appClickable(component = "MessageAction", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Preview
@Composable
private fun MessageActionPopupPreview() {
    AppTheme {
        MessageActionPopup(
            message = ChatMessage(
                id = "preview",
                conversationId = "conv",
                senderId = "me",
                senderName = "我",
                senderAvatar = null,
                body = MessageBody.Text("消息内容"),
                timestamp = Clock.System.now().toEpochMilliseconds(),
                isMine = true,
                status = SendStatus.SENT
            ),
            onDismiss = {},
            onCopy = {},
            onRecall = {},
            onDelete = {}
        )
    }
}
