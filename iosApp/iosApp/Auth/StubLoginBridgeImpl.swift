// Stub implementation — SDK removed for open-source showcase
//
// Architecture: Swift bridge conforming to Kotlin LoginBridge (expect/actual).
// In production, this delegates to the Auth SDK for OAuth
// (Google, Apple, Email) and token management.

import Foundation
import Shared

final class StubLoginBridgeImpl: NSObject, LoginBridge {
    private var cachedAccessToken: String?
    private var cachedRefreshToken: String?
    private weak var bridgeListener: (any Shared.LoginBridgeListener)?

    override init() {
        super.init()
    }

    func setListener(listener: (any Shared.LoginBridgeListener)?) {
        self.bridgeListener = listener
    }

    func handleOpenURL(_ url: URL) -> Bool {
        return false
    }

    func isLoggedIn() -> Bool {
        if let token = cachedAccessToken, !token.isEmpty {
            return true
        }
        return false
    }

    func getAccessToken() -> String? {
        return cachedAccessToken
    }

    func login(type: Shared.LoginType, callback: any Shared.LoginTokenCallback) {
        callback.onFailure(message: "Login SDK not available (stub)")
    }

    func sendEmailCode(email: String, callback: any Shared.LoginUnitCallback) {
        callback.onFailure(message: "Login SDK not available (stub)")
    }

    func verifyEmailCode(email: String, code: String, callback: any Shared.LoginTokenCallback) {
        callback.onFailure(message: "Login SDK not available (stub)")
    }

    func refreshToken(callback: any Shared.LoginTokenCallback) {
        callback.onFailure(message: "Login SDK not available (stub)")
    }

    func logout(callback: any Shared.LoginUnitCallback) {
        cachedAccessToken = nil
        cachedRefreshToken = nil
        callback.onSuccess()
    }
}
