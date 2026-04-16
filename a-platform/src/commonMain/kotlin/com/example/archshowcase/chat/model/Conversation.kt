package com.example.archshowcase.chat.model

import androidx.compose.runtime.Immutable
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Conversation(
    val id: String,
    val name: String,
    val memberAvatars: List<String>,
    val memberCount: Int,
    val lastActiveTime: Long,
    val unreadCount: Int,
    val lastMsgPreview: String? = null,
    val lastMsgBodyType: String? = null,
    val lastMsgSenderName: String? = null,
    val isTyping: Boolean = false,
    val typingUserName: String? = null
) : JvmSerializable
