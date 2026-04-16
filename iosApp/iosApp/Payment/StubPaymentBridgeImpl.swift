// Stub implementation — SDK removed for open-source showcase
//
// Architecture: Swift bridge conforming to Kotlin PaymentBridge (expect/actual).
// In production, this delegates to the Pay SDK for in-app purchases.

import Foundation
import Shared

final class StubPaymentBridgeImpl: NSObject, PaymentBridge {

    func initialize(scene: String) {
        // Stub: SDK initialization removed
    }

    func purchase(
        productId: String,
        planId: String,
        method: String,
        callback: any PaymentBridgeCallback
    ) {
        callback.onError(code: -1, message: "Payment SDK not available (stub)")
    }

    func logout() {
        // Stub: no-op
    }

    func destroy() {
        // Stub: no-op
    }
}
