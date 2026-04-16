// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.rtc

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.rtc.model.ChannelConfig
import com.example.archshowcase.rtc.model.RemoteUser
import com.example.archshowcase.rtc.model.RtcConfig
import com.example.archshowcase.rtc.model.RtcRole
import com.example.archshowcase.rtc.model.RtcState
import com.example.archshowcase.rtc.service.RtcService as AppRtcService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidRtcService : AppRtcService {

    private val _stateFlow = MutableStateFlow<RtcState>(RtcState.Idle)
    override val stateFlow: StateFlow<RtcState> = _stateFlow.asStateFlow()

    private val _remoteUsersFlow = MutableStateFlow<List<RemoteUser>>(emptyList())
    override val remoteUsersFlow: StateFlow<List<RemoteUser>> = _remoteUsersFlow.asStateFlow()

    override fun initialize(config: RtcConfig) {
        _stateFlow.value = RtcState.Initializing
        // Stub: RTC SDK not available
        Log.d(TAG) { "Stub: RTC SDK not initialized" }
        _stateFlow.value = RtcState.Ready
    }

    override fun login(uid: String) {
        // Stub: RTC SDK not available
        Log.d(TAG) { "Stub: RTC login (no-op, uid=$uid)" }
    }

    override suspend fun joinChannel(config: ChannelConfig): Result<Unit> {
        // Stub: RTC SDK not available
        return Result.failure(NotImplementedError("RTC SDK stub"))
    }

    override suspend fun leaveChannel(): Result<Unit> {
        _stateFlow.value = RtcState.Ready
        _remoteUsersFlow.value = emptyList()
        return Result.success(Unit)
    }

    override fun setRole(role: RtcRole) {
        // Stub: no-op
    }

    override fun enableLocalVideo(enable: Boolean) {
        // Stub: no-op
    }

    override fun muteLocalAudioStream(mute: Boolean) {
        // Stub: no-op
    }

    override fun switchCamera() {
        // Stub: no-op
    }

    override fun destroy() {
        _stateFlow.value = RtcState.Idle
        _remoteUsersFlow.value = emptyList()
    }

    companion object {
        private const val TAG = "AndroidRtcService"
    }
}
