package com.example.archshowcase.rtc

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.rtc.model.ChannelConfig
import com.example.archshowcase.rtc.model.RemoteUser
import com.example.archshowcase.rtc.model.RtcConfig
import com.example.archshowcase.rtc.model.RtcRole
import com.example.archshowcase.rtc.model.RtcState
import com.example.archshowcase.rtc.service.RtcService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockRtcService : RtcService {

    private val _stateFlow = MutableStateFlow<RtcState>(RtcState.Idle)
    override val stateFlow: StateFlow<RtcState> = _stateFlow.asStateFlow()

    private val _remoteUsersFlow = MutableStateFlow<List<RemoteUser>>(emptyList())
    override val remoteUsersFlow: StateFlow<List<RemoteUser>> = _remoteUsersFlow.asStateFlow()

    override fun initialize(config: RtcConfig) {
        Log.d(TAG) { "Mock RTC initialized: vendor=${config.vendor}" }
        _stateFlow.value = RtcState.Ready
    }

    override fun login(uid: String) {
        Log.d(TAG) { "Mock RTC login: uid=$uid" }
    }

    override suspend fun joinChannel(config: ChannelConfig): Result<Unit> {
        Log.d(TAG) { "Mock RTC joinChannel: channelId=${config.channelId}" }
        _stateFlow.value = RtcState.Joined(config.channelId)
        return Result.success(Unit)
    }

    override suspend fun leaveChannel(): Result<Unit> {
        Log.d(TAG) { "Mock RTC leaveChannel" }
        _stateFlow.value = RtcState.Ready
        _remoteUsersFlow.value = emptyList()
        return Result.success(Unit)
    }

    override fun setRole(role: RtcRole) {
        Log.d(TAG) { "Mock RTC setRole: $role" }
    }

    override fun enableLocalVideo(enable: Boolean) {
        Log.d(TAG) { "Mock RTC enableLocalVideo: $enable" }
    }

    override fun muteLocalAudioStream(mute: Boolean) {
        Log.d(TAG) { "Mock RTC muteLocalAudioStream: $mute" }
    }

    override fun switchCamera() {
        Log.d(TAG) { "Mock RTC switchCamera" }
    }

    override fun destroy() {
        Log.d(TAG) { "Mock RTC destroy" }
        _stateFlow.value = RtcState.Idle
        _remoteUsersFlow.value = emptyList()
    }

    companion object {
        private const val TAG = "MockRtcService"
    }
}
