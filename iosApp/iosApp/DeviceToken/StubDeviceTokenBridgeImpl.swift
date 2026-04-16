// Stub implementation — SDK removed for open-source showcase
//
// Architecture: Swift bridge conforming to Kotlin DeviceTokenBridge (expect/actual).
// In production, this delegates to the Risk SDK for device
// fingerprinting and risk control.

import Foundation
import Shared

class StubDeviceTokenBridgeImpl: DeviceTokenBridge {
    func initialize(appKey: String) {
        // Stub: SDK initialization removed
    }

    func getUmid() -> String? {
        return nil
    }

    func sign(fields: [String: String]) -> String? {
        return nil
    }

    func reportAction(action: String) {
        // Stub: no-op
    }
}
