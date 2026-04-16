package com.example.archshowcase.rtc.service

import com.example.archshowcase.rtc.model.ChannelConfig
import com.example.archshowcase.rtc.model.RemoteUser
import com.example.archshowcase.rtc.model.RtcConfig
import com.example.archshowcase.rtc.model.RtcRole
import com.example.archshowcase.rtc.model.RtcState
import kotlinx.coroutines.flow.StateFlow

interface RtcService {
    val stateFlow: StateFlow<RtcState>
    val remoteUsersFlow: StateFlow<List<RemoteUser>>

    fun initialize(config: RtcConfig)
    fun login(uid: String)

    suspend fun joinChannel(config: ChannelConfig): Result<Unit>
    suspend fun leaveChannel(): Result<Unit>

    fun setRole(role: RtcRole)
    fun enableLocalVideo(enable: Boolean)
    fun muteLocalAudioStream(mute: Boolean)
    fun switchCamera()

    fun destroy()
}
