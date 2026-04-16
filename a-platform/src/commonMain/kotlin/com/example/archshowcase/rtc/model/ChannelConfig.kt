package com.example.archshowcase.rtc.model

data class ChannelConfig(
    val channelId: String,
    val token: String,
    val uid: Long,
    val role: RtcRole = RtcRole.AUDIENCE,
    val enableVideo: Boolean = false,
)
