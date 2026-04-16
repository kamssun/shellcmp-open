package com.example.archshowcase.rtc.model

data class VideoConfig(
    val width: Int = 360,
    val height: Int = 640,
    val frameRate: Int = 15,
    val bitrate: Int = 800,
)
