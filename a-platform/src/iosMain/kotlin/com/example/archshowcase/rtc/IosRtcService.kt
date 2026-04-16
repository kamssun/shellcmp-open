package com.example.archshowcase.rtc

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.getRtcBridgeOrNull
import com.example.archshowcase.rtc.model.ChannelConfig
import com.example.archshowcase.rtc.model.RemoteUser
import com.example.archshowcase.rtc.model.RtcConfig
import com.example.archshowcase.rtc.model.RtcRole
import com.example.archshowcase.rtc.model.RtcState
import com.example.archshowcase.rtc.service.RtcService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosRtcService : RtcService {

    private val _stateFlow = MutableStateFlow<RtcState>(RtcState.Idle)
    override val stateFlow: StateFlow<RtcState> = _stateFlow.asStateFlow()

    private val _remoteUsersFlow = MutableStateFlow<List<RemoteUser>>(emptyList())
    override val remoteUsersFlow: StateFlow<List<RemoteUser>> = _remoteUsersFlow.asStateFlow()

    private val stateCallback = object : RtcStateCallback {
        override fun onStateChanged(
            stateType: String,
            channelId: String?,
            errorCode: Int,
            errorMessage: String?,
        ) {
            _stateFlow.value = when (stateType) {
                STATE_IDLE -> RtcState.Idle
                STATE_INITIALIZING -> RtcState.Initializing
                STATE_READY -> RtcState.Ready
                STATE_JOINED -> RtcState.Joined(channelId.orEmpty())
                STATE_ERROR -> RtcState.Error(errorCode, errorMessage.orEmpty())
                else -> RtcState.Idle
            }
        }
    }

    private val remoteUserCallback = object : RtcRemoteUserCallback {
        override fun onUserJoined(uid: String) {
            _remoteUsersFlow.update { current ->
                if (current.any { it.uid == uid }) current
                else current + RemoteUser(uid)
            }
        }

        override fun onUserLeft(uid: String) {
            _remoteUsersFlow.update { current ->
                current.filter { it.uid != uid }
            }
        }
    }

    override fun initialize(config: RtcConfig) {
        val bridge = getRtcBridgeOrNull()
        if (bridge == null) {
            Log.w(TAG) { "RtcBridge not set, skipping initialization" }
            _stateFlow.value = RtcState.Error(-1, "RtcBridge not available")
            return
        }
        bridge.setStateCallback(stateCallback)
        bridge.setRemoteUserCallback(remoteUserCallback)
        bridge.initialize(appId = config.appId, vendor = config.vendor.name)
    }

    override fun login(uid: String) {
        // iOS SDK login 在 Bridge.initialize 中完成
    }

    override suspend fun joinChannel(config: ChannelConfig): Result<Unit> {
        val bridge = getRtcBridgeOrNull()
            ?: return Result.failure(IllegalStateException("RtcBridge not set"))

        _remoteUsersFlow.value = emptyList()

        return suspendCancellableCoroutine { cont ->
            bridge.joinChannel(
                channelId = config.channelId,
                token = config.token,
                uid = config.uid,
                role = config.role.name,
                enableVideo = config.enableVideo,
                callback = object : RtcBridgeCallback {
                    override fun onSuccess() {
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }

                    override fun onError(code: Int, message: String) {
                        if (cont.isActive) cont.resume(Result.failure(RuntimeException(message)))
                    }
                }
            )
        }
    }

    override suspend fun leaveChannel(): Result<Unit> {
        getRtcBridgeOrNull()?.leaveChannel()
        _remoteUsersFlow.value = emptyList()
        return Result.success(Unit)
    }

    override fun setRole(role: RtcRole) {
        getRtcBridgeOrNull()?.setRole(role.name)
    }

    override fun enableLocalVideo(enable: Boolean) {
        getRtcBridgeOrNull()?.enableLocalVideo(enable)
    }

    override fun muteLocalAudioStream(mute: Boolean) {
        getRtcBridgeOrNull()?.muteLocalAudioStream(mute)
    }

    override fun switchCamera() {
        getRtcBridgeOrNull()?.switchCamera()
    }

    override fun destroy() {
        getRtcBridgeOrNull()?.let { bridge ->
            bridge.setStateCallback(null)
            bridge.setRemoteUserCallback(null)
            bridge.destroy()
        }
        _stateFlow.value = RtcState.Idle
        _remoteUsersFlow.value = emptyList()
    }

    companion object {
        private const val TAG = "IosRtcService"
        private const val STATE_IDLE = "idle"
        private const val STATE_INITIALIZING = "initializing"
        private const val STATE_READY = "ready"
        private const val STATE_JOINED = "joined"
        private const val STATE_ERROR = "error"
    }
}
