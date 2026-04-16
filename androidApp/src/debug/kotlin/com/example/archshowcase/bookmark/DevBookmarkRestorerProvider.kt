package com.example.archshowcase.bookmark

import com.example.archshowcase.core.trace.bookmark.DevBookmarkHolder
import com.example.archshowcase.core.trace.bookmark.DevBookmarkRestorer

/**
 * debug 源集提供 DevBookmarkRestorer 实例
 *
 * 通过反射从 MainActivity 调用，release 包中不存在该类。
 * 同时将 storage 注册到 [DevBookmarkHolder] 供 Component 层使用。
 */
object DevBookmarkRestorerProvider {

    private val storage = AndroidDevBookmarkStorage()
    private val restorer = DevBookmarkRestorer(storage)

    init {
        DevBookmarkHolder.storage = storage
    }

    fun restoreIfNeeded(): Boolean {
        val result = restorer.restoreIfNeeded()
        DevBookmarkHolder.pendingMessage = restorer.consumeMessage()
        return result
    }
}
