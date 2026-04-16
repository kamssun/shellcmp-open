package com.example.archshowcase.core.util

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.View
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

actual object ScreenshotCapture {
    actual fun capture(): ByteArray? = try {
        val activity = ContextProvider.current
        if (activity == null) {
            Log.e("Screenshot") { "capture: ContextProvider.current is null" }
            return null
        }
        val window = activity.window
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        if (rootView == null || !rootView.isLaidOut) {
            Log.e("Screenshot") { "capture: rootView null or not laid out" }
            return null
        }

        val width = rootView.width
        val height = rootView.height
        if (width <= 0 || height <= 0) {
            Log.e("Screenshot") { "capture: invalid size ${width}x${height}" }
            return null
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // PixelCopy：支持 hardware bitmap 的 View 截图
            val latch = CountDownLatch(1)
            var copyResult = PixelCopy.ERROR_UNKNOWN

            val location = IntArray(2)
            rootView.getLocationInWindow(location)

            // 用后台线程接收回调，避免主线程死锁
            val thread = HandlerThread("PixelCopyThread").apply { start() }
            val handler = Handler(thread.looper)

            PixelCopy.request(
                window,
                android.graphics.Rect(location[0], location[1], location[0] + width, location[1] + height),
                bitmap,
                { result ->
                    copyResult = result
                    latch.countDown()
                },
                handler
            )

            if (!latch.await(3, TimeUnit.SECONDS)) {
                Log.e("Screenshot") { "capture: PixelCopy timeout" }
                thread.quitSafely()
                bitmap.recycle()
                return null
            }
            thread.quitSafely()

            if (copyResult != PixelCopy.SUCCESS) {
                Log.e("Screenshot") { "capture: PixelCopy failed with code $copyResult" }
                bitmap.recycle()
                return null
            }
        } else {
            // API < 26 回退到 Canvas 绘制
            val canvas = android.graphics.Canvas(bitmap)
            rootView.draw(canvas)
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        bitmap.recycle()
        val bytes = stream.toByteArray()
        Log.d("Screenshot") { "capture: success, ${bytes.size} bytes (${width}x${height})" }
        bytes
    } catch (e: Exception) {
        Log.e("Screenshot") { "capture failed: ${e::class.simpleName}: ${e.message}" }
        null
    }
}
