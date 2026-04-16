package com.example.archshowcase.chat.model

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import kotlinx.serialization.Serializable

@Serializable
enum class SendStatus : JvmSerializable {
    SENDING,
    SENT,
    FAILED
}
