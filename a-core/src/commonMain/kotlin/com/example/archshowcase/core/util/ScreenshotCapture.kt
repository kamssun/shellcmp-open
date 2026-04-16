package com.example.archshowcase.core.util

/**
 * 平台截图能力
 *
 * Android: 通过 ContextProvider.current 获取 Activity 截图
 * 其他平台: 返回 null
 */
expect object ScreenshotCapture {
    fun capture(): ByteArray?
}
