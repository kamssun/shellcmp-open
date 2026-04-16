package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.core.analytics.appClickable
import com.example.archshowcase.core.analytics.appCombinedClickable
import com.example.archshowcase.core.analytics.appKeyboardActions
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.chat.room.InputMode
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_btn_send
import com.example.archshowcase.resources.chat_input_hold_to_talk
import com.example.archshowcase.resources.chat_input_placeholder

@Composable
fun ChatInputBar(
    inputMode: InputMode,
    text: String,
    onTextChange: (String) -> Unit,
    onSendText: (String) -> Unit,
    onSendVoice: (Int) -> Unit,
    onToggleInputMode: () -> Unit,
    onToggleEmoji: () -> Unit,
    onTogglePlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Voice / Keyboard toggle
        Box(
            modifier = Modifier
                .size(36.dp)
                .appClickable(component = "ChatInputBar:InputMode", onClick = onToggleInputMode),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = if (inputMode == InputMode.TEXT) "\uD83C\uDF99" else "⌨",
                style = AppTheme.typography.titleMedium
            )
        }

        // Input area
        if (inputMode == InputMode.TEXT) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 100.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppTheme.colors.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                textStyle = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.onSurface),
                cursorBrush = SolidColor(AppTheme.colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = appKeyboardActions(
                    component = "ChatInputBar:ImeSend",
                    onSend = {
                        if (text.isNotBlank()) {
                            onSendText(text)
                            onTextChange("")
                        }
                    },
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            AppText(
                                text = tr(Res.string.chat_input_placeholder),
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            // Voice input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppTheme.colors.surfaceVariant)
                    .appCombinedClickable(
                        component = "ChatInputBar:Voice",
                        onLongClick = { onSendVoice(0) },
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = tr(Res.string.chat_input_hold_to_talk),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant
                )
            }
        }

        // Emoji button
        Box(
            modifier = Modifier
                .size(36.dp)
                .appClickable(component = "ChatInputBar:Emoji", onClick = onToggleEmoji),
            contentAlignment = Alignment.Center
        ) {
            AppText(text = "😊", style = AppTheme.typography.titleMedium)
        }

        // Plus button
        Box(
            modifier = Modifier
                .size(36.dp)
                .appClickable(component = "ChatInputBar:Plus", onClick = onTogglePlus),
            contentAlignment = Alignment.Center
        ) {
            AppText(text = "➕", style = AppTheme.typography.titleMedium)
        }

        // Send button (only visible when text is not empty)
        if (inputMode == InputMode.TEXT && text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppTheme.colors.primary)
                    .appClickable(
                        component = "ChatInputBar:Send",
                        onClick = {
                            onSendText(text)
                            onTextChange("")
                        },
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = tr(Res.string.chat_btn_send),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.onPrimary
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChatInputBarPreview() {
    AppTheme {
        ChatInputBar(
            inputMode = InputMode.TEXT,
            text = "",
            onTextChange = {},
            onSendText = {},
            onSendVoice = {},
            onToggleInputMode = {},
            onToggleEmoji = {},
            onTogglePlus = {}
        )
    }
}
