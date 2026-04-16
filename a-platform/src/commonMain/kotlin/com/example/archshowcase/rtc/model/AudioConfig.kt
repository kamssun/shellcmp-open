package com.example.archshowcase.rtc.model

data class AudioConfig(
    val sampleRate: Int = 48000,
    val channels: Int = 1,
    val bitrate: Int = 48,
)
