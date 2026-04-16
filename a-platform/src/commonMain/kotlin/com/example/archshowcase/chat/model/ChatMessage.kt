package com.example.archshowcase.chat.model

import androidx.compose.runtime.Immutable
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val body: MessageBody,
    val timestamp: Long,
    val isMine: Boolean,
    val status: SendStatus = SendStatus.SENT,
    val isRecalled: Boolean = false
) : JvmSerializable
