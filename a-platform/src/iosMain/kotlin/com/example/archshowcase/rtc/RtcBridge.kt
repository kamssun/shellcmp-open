package com.example.archshowcase.rtc

interface RtcBridgeCallback {
    fun onSuccess()
    fun onError(code: Int, message: String)
}

interface RtcRemoteUserCallback {
    fun onUserJoined(uid: String)
    fun onUserLeft(uid: String)
}

interface RtcStateCallback {
    fun onStateChanged(
        stateType: String,
        channelId: String?,
        errorCode: Int,
        errorMessage: String?,
    )
}

interface RtcBridge {
    fun initialize(appId: String, vendor: String)
    fun joinChannel(
        channelId: String,
        token: String,
        uid: Long,
        role: String,
        enableVideo: Boolean,
        callback: RtcBridgeCallback,
    )
    fun leaveChannel()
    fun setRole(role: String)
    fun enableLocalVideo(enable: Boolean)
    fun muteLocalAudioStream(mute: Boolean)
    fun switchCamera()
    fun destroy()

    fun setStateCallback(callback: RtcStateCallback?)
    fun setRemoteUserCallback(callback: RtcRemoteUserCallback?)
}
