package com.example.archshowcase.rtc.model

data class RtcConfig(
    val appId: String,
    val vendor: RtcVendor = RtcVendor.AGORA,
    val debug: Boolean = false,
)
