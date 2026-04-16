import Foundation
import Shared

final class StubUserBridgeImpl: NSObject, UserBridge {

    func fetchProfile(callback: any Shared.UserProfileCallback) {
        // TODO: 后端就绪后接通用户信息 API
        callback.onFailure(message: "用户信息获取暂未开放")
    }
}
