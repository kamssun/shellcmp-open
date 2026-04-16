// Stub implementation — SDK removed for open-source showcase
//
// Architecture: Swift bridge conforming to Kotlin ImBridge (expect/actual).
// In production, this delegates to the IM SDK for real-time messaging.

import Foundation
import Shared

final class StubImBridgeImpl: NSObject, ImBridge {
    private var statusCallback: (any Shared.ImStatusCallback)?
    private var messageCallback: (any ImMessageCallback)?
    private var roomStatusCallback: (any ImRoomStatusCallback)?

    override init() {
        statusCallback = nil
        messageCallback = nil
        roomStatusCallback = nil
        super.init()
    }

    func initialize(
        isDebug: Bool,
        apiKey: String,
        codeTag: String,
        xlogKey: String,
        memberId: String,
        nickName: String,
        token: String,
        imConfig: String,
        deviceId: String
    ) {
        // Stub: SDK initialization removed
    }

    func login(callback: any ImBridgeCallback) {
        callback.onError(code: -1, message: "IM SDK not available (stub)")
    }

    func logout() {
        // Stub: no-op
    }

    func isLoggedIn() -> Bool {
        return false
    }

    func destroy() {
        // Stub: no-op
    }

    func setStatusCallback(callback: (any Shared.ImStatusCallback)?) {
        self.statusCallback = callback
    }

    func setMessageCallback(callback: (any ImMessageCallback)?) {
        self.messageCallback = callback
    }

    func setRoomStatusCallback(callback: (any ImRoomStatusCallback)?) {
        self.roomStatusCallback = callback
    }

    func enterRoom(roomId: String, callback: any ImBridgeCallback) {
        callback.onError(code: -1, message: "IM SDK not available (stub)")
    }

    func leaveRoom(roomId: String) {
        // Stub: no-op
    }

    func pullHistory(convId: String, seqIdGte: Int64, seqIdLte: Int64, callback: any ImHistoryCallback) {
        callback.onError(code: -1, message: "IM SDK not available (stub)")
    }
}
