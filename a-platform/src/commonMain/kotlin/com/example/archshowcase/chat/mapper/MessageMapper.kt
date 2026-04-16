package com.example.archshowcase.chat.mapper

import com.example.archshowcase.chat.db.Chat_message
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.SendStatus
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun Chat_message.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    conversationId = conversation_id,
    senderId = sender_id,
    senderName = sender_name,
    senderAvatar = sender_avatar,
    body = deserializeBody(body_type, body_json),
    timestamp = timestamp,
    isMine = is_mine != 0L,
    status = try { SendStatus.valueOf(status) } catch (e: Exception) { Log.w(TAG) { "Unknown SendStatus: $status" }; SendStatus.SENT },
    isRecalled = is_recalled != 0L
)

fun ChatMessage.toBodyType(): String = when (body) {
    is MessageBody.Text -> "text"
    is MessageBody.Image -> "image"
    is MessageBody.Sticker -> "sticker"
    is MessageBody.Voice -> "voice"
    is MessageBody.Video -> "video"
    is MessageBody.Gift -> "gift"
    is MessageBody.Broadcast -> "broadcast"
    is MessageBody.System -> "system"
    is MessageBody.Unknown -> "unknown"
}

fun ChatMessage.toBodyJson(): String = json.encodeToString(MessageBody.serializer(), body)

internal fun deserializeBody(type: String, bodyJson: String): MessageBody = try {
    json.decodeFromString(MessageBody.serializer(), bodyJson)
} catch (e: Exception) {
    Log.w(TAG) { "Failed to deserialize body type=$type: ${e.message}" }
    MessageBody.Unknown(rawType = type, rawJson = bodyJson)
}

/** 从 MessageBody 提取列表预览文本（存入 conversation.last_msg_preview） */
fun MessageBody.toPreviewText(): String = when (this) {
    is MessageBody.Text -> text
    is MessageBody.System -> text
    is MessageBody.Voice -> "${durationMs / 1000}" // 整数秒，消费端 toLongOrNull() 解析
    is MessageBody.Gift -> name
    is MessageBody.Broadcast -> text
    else -> ""
}

/** 列表预览用 body type（区分 image / image_gif） */
fun MessageBody.toSummaryBodyType(): String = when (this) {
    is MessageBody.Image -> if (isGif) BodyTypeKey.IMAGE_GIF else BodyTypeKey.IMAGE
    is MessageBody.Text -> BodyTypeKey.TEXT
    is MessageBody.Sticker -> BodyTypeKey.STICKER
    is MessageBody.Voice -> BodyTypeKey.VOICE
    is MessageBody.Video -> BodyTypeKey.VIDEO
    is MessageBody.Gift -> BodyTypeKey.GIFT
    is MessageBody.Broadcast -> BodyTypeKey.BROADCAST
    is MessageBody.System -> BodyTypeKey.SYSTEM
    is MessageBody.Unknown -> BodyTypeKey.UNKNOWN
}

/** 列表预览用 body type 常量，消费端（ConversationListContent）共享 */
object BodyTypeKey {
    const val TEXT = "text"
    const val IMAGE = "image"
    const val IMAGE_GIF = "image_gif"
    const val STICKER = "sticker"
    const val VOICE = "voice"
    const val VIDEO = "video"
    const val GIFT = "gift"
    const val BROADCAST = "broadcast"
    const val SYSTEM = "system"
    const val UNKNOWN = "unknown"
}

private const val TAG = "MessageMapper"
