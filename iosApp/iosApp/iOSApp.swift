import SwiftUI
import Shared

@main
struct iOSApp: App {
    private let loginBridge = StubLoginBridgeImpl()
    private let imBridge = StubImBridgeImpl()
    private let rtcBridge = StubRtcBridgeImpl()
    private let paymentBridge = StubPaymentBridgeImpl()
    private let userBridge = StubUserBridgeImpl()
    private let deviceTokenBridge = StubDeviceTokenBridgeImpl()
    private let attributionBridge = AdjustBridgeImpl()

    init() {
        MainViewControllerKt.doInitKoin()
        BridgeRegistryKt.setLoginBridge(bridge: loginBridge)
        BridgeRegistryKt.setImBridge(bridge: imBridge)
        BridgeRegistryKt.setRtcBridge(bridge: rtcBridge)
        BridgeRegistryKt.setPaymentBridge(bridge: paymentBridge)
        BridgeRegistryKt.setUserBridge(bridge: userBridge)
        BridgeRegistryKt.setDeviceTokenBridge(bridge: deviceTokenBridge)
        BridgeRegistryKt.setAttributionBridge(bridge: attributionBridge)
        let appKey = Bundle.main.object(forInfoDictionaryKey: InfoPlistKey.appKey) as? String ?? ""
        deviceTokenBridge.initialize(appKey: appKey)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = loginBridge.handleOpenURL(url)
                }
        }
    }
}
