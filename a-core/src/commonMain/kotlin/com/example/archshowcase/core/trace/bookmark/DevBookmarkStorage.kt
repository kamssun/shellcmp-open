package com.example.archshowcase.core.trace.bookmark

/**
 * 开发书签存储接口
 *
 * 保存/加载当前 TTE 快照到固定路径，用于重启后自动恢复界面状态。
 */
interface DevBookmarkStorage {
    fun save(bytes: ByteArray): Boolean
    fun load(): ByteArray?
    fun delete(): Boolean
    fun exists(): Boolean
}

/**
 * DevBookmarkStorage 持有者
 *
 * debug 构建设置实例，release 构建为 null。
 * Component 层通过此 holder 访问 storage。
 */
object DevBookmarkHolder {
    var storage: DevBookmarkStorage? = null

    /** 启动恢复失败时由 DevBookmarkRestorer 设置，Component 初始化时消费 */
    var pendingMessage: String? = null
}
