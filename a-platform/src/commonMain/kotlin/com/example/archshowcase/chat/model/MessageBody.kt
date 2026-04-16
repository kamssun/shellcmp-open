package com.example.archshowcase.chat.model

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageBody : JvmSerializable {

    @Serializable
    @SerialName("text")
    data class Text(val text: String) : MessageBody

    @Serializable
    @SerialName("image")
    data class Image(
        val url: String,
        val width: Int,
        val height: Int,
        val isGif: Boolean = false
    ) : MessageBody

    @Serializable
    @SerialName("sticker")
    data class Sticker(val stickerId: String, val url: String) : MessageBody

    @Serializable
    @SerialName("voice")
    data class Voice(val url: String, val durationMs: Int) : MessageBody

    @Serializable
    @SerialName("video")
    data class Video(
        val url: String,
        val thumbnailUrl: String,
        val durationMs: Int
    ) : MessageBody

    @Serializable
    @SerialName("gift")
    data class Gift(val giftId: String, val name: String, val count: Int) : MessageBody

    @Serializable
    @SerialName("broadcast")
    data class Broadcast(val text: String) : MessageBody

    @Serializable
    @SerialName("system")
    data class System(val text: String) : MessageBody

    @Serializable
    @SerialName("unknown")
    data class Unknown(val rawType: String, val rawJson: String) : MessageBody
}
