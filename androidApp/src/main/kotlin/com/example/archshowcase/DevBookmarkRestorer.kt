package com.example.archshowcase

import android.util.Log

/**
 * 开发书签恢复入口
 *
 * 通过反射调用 debug-only 的 DevBookmarkRestorerProvider.restoreIfNeeded()，
 * 在 VerificationRestorer 之后调用，仅当 verificationMode 未激活时生效。
 */
object DevBookmarkRestorer {

    fun restore() {
        if (!BuildConfig.DEBUG) return
        try {
            val provider = Class.forName("com.example.archshowcase.bookmark.DevBookmarkRestorerProvider")
            val instance = provider.getField("INSTANCE").get(null)
            val method = provider.getMethod("restoreIfNeeded")
            method.invoke(instance)
        } catch (_: ClassNotFoundException) {
            // debug 包但类不存在，忽略
        } catch (e: Exception) {
            Log.w("DevBookmarkRestorer", "Restore failed: ${e.message}")
        }
    }
}
