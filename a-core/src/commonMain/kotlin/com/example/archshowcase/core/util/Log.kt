package com.example.archshowcase.core.util

import co.touchlab.kermit.Logger
import com.example.archshowcase.core.isDebug

fun interface LogWriter {
    fun write(severity: LogSeverity, tag: String, message: String, throwable: Throwable?)
}

enum class LogSeverity { DEBUG, INFO, WARN, ERROR, CRITICAL }

object Log {
    private val writers = mutableListOf<LogWriter>()
    private var minSeverity = LogSeverity.DEBUG

    /**
     * Release 全量日志开关。
     * true → d() / i() 在 release 也输出；false（默认）→ 仅 debug 输出。
     */
    var enableReleaseLog: Boolean = false

    fun install(vararg logWriters: LogWriter) {
        writers.addAll(logWriters)
    }

    fun setMinSeverity(severity: LogSeverity) {
        minSeverity = severity
    }

    fun d(tag: String = "", message: () -> String) {
        if (!enableReleaseLog && !isDebug()) return
        if (minSeverity > LogSeverity.DEBUG) return
        val msg = message()
        Logger.d(tag = tag) { msg }
        dispatch(LogSeverity.DEBUG, tag, msg, null)
    }

    fun i(tag: String = "", message: () -> String) {
        if (!enableReleaseLog && !isDebug()) return
        if (minSeverity > LogSeverity.INFO) return
        val msg = message()
        Logger.i(tag = tag) { msg }
        dispatch(LogSeverity.INFO, tag, msg, null)
    }

    fun w(tag: String = "", message: () -> String) {
        if (minSeverity > LogSeverity.WARN) return
        val msg = message()
        Logger.w(tag = tag) { msg }
        dispatch(LogSeverity.WARN, tag, msg, null)
    }

    fun e(tag: String = "", throwable: Throwable? = null, message: () -> String) {
        if (minSeverity > LogSeverity.ERROR) return
        val msg = message()
        Logger.e(throwable, tag = tag) { msg }
        dispatch(LogSeverity.ERROR, tag, msg, throwable)
    }

    fun wtf(tag: String = "", throwable: Throwable? = null, message: () -> String) {
        val msg = message()
        Logger.a(throwable, tag = tag) { msg }
        dispatch(LogSeverity.CRITICAL, tag, msg, throwable)
    }

    private fun dispatch(severity: LogSeverity, tag: String, message: String, throwable: Throwable?) {
        writers.forEach { it.write(severity, tag, message, throwable) }
    }
}
