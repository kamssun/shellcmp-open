import Foundation

/// Info.plist 配置 key 常量，消除跨文件的字面量重复
enum InfoPlistKey {
    static let serverUrl = "APP_SERVER_URL"
    static let appKey = "APP_KEY"
    static let googleClientId = "GOOGLE_CLIENT_ID"
    static let googleServerClientId = "GOOGLE_SERVER_CLIENT_ID"
}
