package com.example.archshowcase.im.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    @SerialName("msg_type") val msgType: Int,
    @SerialName("room_id") val roomId: String,
    val content: String
)
