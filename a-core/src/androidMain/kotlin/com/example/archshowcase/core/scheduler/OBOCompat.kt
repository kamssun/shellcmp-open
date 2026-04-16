package com.example.archshowcase.core.scheduler

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.example.archshowcase.core.AppConfig

/**
 * Handler 方法拦截的运行时转发层
 *
 * ASM 在编译期将三方 SDK 的 Handler 调用重写为此类的静态方法。
 * - 主线程 Handler + OBO 开启 → 通过 OBO 调度
 * - 否则 → 透传原始 Handler 调用
 *
 * 定时交给系统 MessageQueue，执行交给 OBO。
 *
 * 注意：此类及 com.example.archshowcase.core.scheduler 包下的所有类
 * 均被 ASM Transform 排除，内部的 Handler 调用不会被改写。
 */
object OBOCompat {

    private inline val enabled get() = AppConfig.useOBOScheduler

    private fun isMain(handler: Handler): Boolean =
        handler.looper == Looper.getMainLooper()

    // ── Runnable 系列：立即 ─────────────────────────

    @JvmStatic
    fun post(handler: Handler, r: Runnable, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            OBOScheduler.post(tag) { r.run() }
            return true
        }
        return handler.post(r)
    }

    // ── Runnable 系列：定时 ─────────────────────────

    @JvmStatic
    fun postAtTime(handler: Handler, r: Runnable, uptimeMillis: Long, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            handler.postAtTime({ OBOScheduler.post(tag) { r.run() } }, uptimeMillis)
            return true
        }
        return handler.postAtTime(r, uptimeMillis)
    }

    @JvmStatic
    fun postAtTime(
        handler: Handler,
        r: Runnable,
        token: Any?,
        uptimeMillis: Long,
        tag: String,
    ): Boolean {
        if (enabled && isMain(handler)) {
            handler.postAtTime({ OBOScheduler.post(tag) { r.run() } }, token, uptimeMillis)
            return true
        }
        return handler.postAtTime(r, token, uptimeMillis)
    }

    // ── Runnable 系列：延时 ─────────────────────────

    @JvmStatic
    fun postDelayed(handler: Handler, r: Runnable, delayMillis: Long, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            handler.postDelayed({ OBOScheduler.post(tag) { r.run() } }, delayMillis)
            return true
        }
        return handler.postDelayed(r, delayMillis)
    }

    @SuppressLint("NewApi")
    @JvmStatic
    fun postDelayed(
        handler: Handler,
        r: Runnable,
        token: Any?,
        delayMillis: Long,
        tag: String,
    ): Boolean {
        if (enabled && isMain(handler)) {
            handler.postDelayed({ OBOScheduler.post(tag) { r.run() } }, token, delayMillis)
            return true
        }
        return handler.postDelayed(r, token, delayMillis)
    }

    // ── AtFrontOfQueue 系列 ────────────────────────

    @JvmStatic
    fun postAtFrontOfQueue(handler: Handler, r: Runnable, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            OBOScheduler.postFirst(tag) { r.run() }
            return true
        }
        return handler.postAtFrontOfQueue(r)
    }

    @JvmStatic
    fun sendMessageAtFrontOfQueue(handler: Handler, msg: Message, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            OBOScheduler.postFirst(tag) { handler.dispatchMessage(msg) }
            return true
        }
        return handler.sendMessageAtFrontOfQueue(msg)
    }

    // ── Message 系列：立即 ──────────────────────────

    @JvmStatic
    fun sendMessage(handler: Handler, msg: Message, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            OBOScheduler.post(tag) { handler.dispatchMessage(msg) }
            return true
        }
        return handler.sendMessage(msg)
    }

    @JvmStatic
    fun sendEmptyMessage(handler: Handler, what: Int, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            OBOScheduler.post(tag) {
                handler.dispatchMessage(Message.obtain().also { it.what = what })
            }
            return true
        }
        return handler.sendEmptyMessage(what)
    }

    // ── Message 系列：延时 ──────────────────────────

    @JvmStatic
    fun sendMessageDelayed(handler: Handler, msg: Message, delayMillis: Long, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            handler.postDelayed({ OBOScheduler.post(tag) { handler.dispatchMessage(msg) } }, delayMillis)
            return true
        }
        return handler.sendMessageDelayed(msg, delayMillis)
    }

    @JvmStatic
    fun sendEmptyMessageDelayed(handler: Handler, what: Int, delayMillis: Long, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            handler.postDelayed({
                OBOScheduler.post(tag) {
                    handler.dispatchMessage(Message.obtain().also { it.what = what })
                }
            }, delayMillis)
            return true
        }
        return handler.sendEmptyMessageDelayed(what, delayMillis)
    }

    // ── Message 系列：定时 ──────────────────────────

    @JvmStatic
    fun sendMessageAtTime(handler: Handler, msg: Message, uptimeMillis: Long, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            handler.postAtTime(
                { OBOScheduler.post(tag) { handler.dispatchMessage(msg) } },
                uptimeMillis,
            )
            return true
        }
        return handler.sendMessageAtTime(msg, uptimeMillis)
    }

    @JvmStatic
    fun sendEmptyMessageAtTime(handler: Handler, what: Int, uptimeMillis: Long, tag: String): Boolean {
        if (enabled && isMain(handler)) {
            handler.postAtTime({
                OBOScheduler.post(tag) {
                    handler.dispatchMessage(Message.obtain().also { it.what = what })
                }
            }, uptimeMillis)
            return true
        }
        return handler.sendEmptyMessageAtTime(what, uptimeMillis)
    }
}
