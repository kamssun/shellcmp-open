package com.example.archshowcase.im.model

data class ImMessage(
    val id: String,
    val content: String,
    val senderId: String,
    val roomId: String?,
    val timestamp: Long,
    val type: MessageType,
    val rawJson: String? = null
)

enum class MessageType {
    TEXT,
    CUSTOM,
    SYSTEM
}
