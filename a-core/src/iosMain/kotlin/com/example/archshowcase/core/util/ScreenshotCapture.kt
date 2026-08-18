package com.example.archshowcase.core.util

actual object ScreenshotCapture {
    actual fun prepareForCapture() = Unit
    actual fun capture(): ByteArray? = null
}
