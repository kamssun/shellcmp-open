package com.example.archshowcase.rtc.model

sealed interface RtcState {
    data object Idle : RtcState
    data object Initializing : RtcState
    data object Ready : RtcState
    data class Joined(val channelId: String) : RtcState
    data class Error(val code: Int, val message: String) : RtcState
}
