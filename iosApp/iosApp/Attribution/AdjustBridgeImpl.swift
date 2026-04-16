import Foundation
import Shared

#if canImport(Adjust)
import Adjust
#endif

final class AdjustBridgeImpl: NSObject, AttributionBridge {

    func initialize(appToken: String, isProduction: Bool) {
        #if canImport(Adjust)
        let environment = isProduction
            ? ADJEnvironmentProduction
            : ADJEnvironmentSandbox
        guard let config = ADJConfig(appToken: appToken, environment: environment) else { return }
        Adjust.initSdk(config)
        #endif
    }

    func trackEvent(eventToken: String) {
        #if canImport(Adjust)
        let event = ADJEvent(eventToken: eventToken)
        Adjust.trackEvent(event)
        #endif
    }
}
