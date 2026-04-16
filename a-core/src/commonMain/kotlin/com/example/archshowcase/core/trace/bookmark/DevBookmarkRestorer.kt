package com.example.archshowcase.core.trace.bookmark

import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.verification.TteStateExtractor
import com.example.archshowcase.core.util.Log

/**
 * 开发书签恢复器
 *
 * App 启动时调用 [restoreIfNeeded]，从固定路径读取 TTE 快照并恢复 Store 状态。
 * VF 验证优先：若 verificationMode 已为 true 则跳过。
 * 反序列化失败时静默删除文件并设置 [pendingMessage]。
 */
class DevBookmarkRestorer(private val storage: DevBookmarkStorage) {

    /** 恢复后的待显示消息（UI 层消费后清空） */
    var pendingMessage: String? = null
        private set

    /**
     * @return true 表示成功恢复
     */
    fun restoreIfNeeded(): Boolean {
        if (AppRuntimeState.verificationMode) {
            Log.d(TAG) { "Skipped: verificationMode already active (VF takes priority)" }
            return false
        }

        if (!storage.exists()) return false

        val bytes = storage.load()
        if (bytes == null || bytes.isEmpty()) {
            storage.delete()
            pendingMessage = MSG_EXPIRED
            return false
        }

        val states = TteStateExtractor.extract(bytes).getOrElse {
            Log.w(TAG) { "Deserialization failed: ${it.message}" }
            storage.delete()
            pendingMessage = MSG_EXPIRED
            return false
        }

        if (states.isEmpty()) {
            storage.delete()
            pendingMessage = MSG_EXPIRED
            return false
        }

        RestoreRegistry.clearAllSnapshots()
        states.forEach { (name, state) ->
            RestoreRegistry.updateSnapshotOrCreate(name, state)
        }
        AppRuntimeState.verificationMode = true

        Log.i(TAG) { "Restored ${states.size} stores from bookmark" }
        return true
    }

    /** 消费 pending message（UI 显示后调用） */
    fun consumeMessage(): String? {
        val msg = pendingMessage
        pendingMessage = null
        return msg
    }

    companion object {
        private const val TAG = "DevBookmark"
        const val MSG_EXPIRED = "书签已失效，已自动清除"
    }
}
