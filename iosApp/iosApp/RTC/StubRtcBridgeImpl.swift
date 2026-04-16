// Stub implementation — SDK removed for open-source showcase
//
// Architecture: Swift bridge conforming to Kotlin RtcBridge (expect/actual).
// In production, this delegates to the RTC SDK for real-time
// audio/video communication (backed by Agora).

import Foundation
import Shared

final class StubRtcBridgeImpl: NSObject, RtcBridge {
    private var stateCallback: (any RtcStateCallback)?
    private var remoteUserCallback: (any RtcRemoteUserCallback)?

    override init() {
        stateCallback = nil
        remoteUserCallback = nil
        super.init()
    }

    func initialize(appId: String, vendor: String) {
        stateCallback?.onStateChanged(stateType: "initializing", channelId: nil, errorCode: 0, errorMessage: nil)
        // Stub: SDK initialization removed
        stateCallback?.onStateChanged(stateType: "ready", channelId: nil, errorCode: 0, errorMessage: nil)
    }

    func joinChannel(
        channelId: String,
        token: String,
        uid: Int64,
        role: String,
        enableVideo: Bool,
        callback: any RtcBridgeCallback
    ) {
        callback.onError(code: -1, message: "RTC SDK not available (stub)")
    }

    func leaveChannel() {
        stateCallback?.onStateChanged(stateType: "ready", channelId: nil, errorCode: 0, errorMessage: nil)
    }

    func setRole(role_ role: String) {
        // Stub: no-op
    }

    func enableLocalVideo(enable: Bool) {
        // Stub: no-op
    }

    func muteLocalAudioStream(mute: Bool) {
        // Stub: no-op
    }

    func switchCamera() {
        // Stub: no-op
    }

    func destroy() {
        stateCallback?.onStateChanged(stateType: "idle", channelId: nil, errorCode: 0, errorMessage: nil)
    }

    func setStateCallback(callback: (any RtcStateCallback)?) {
        self.stateCallback = callback
    }

    func setRemoteUserCallback(callback: (any RtcRemoteUserCallback)?) {
        self.remoteUserCallback = callback
    }
}
