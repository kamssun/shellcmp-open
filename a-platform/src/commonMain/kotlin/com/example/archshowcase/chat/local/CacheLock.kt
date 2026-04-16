package com.example.archshowcase.chat.local

/** KMP 线程安全锁：JVM 用 synchronized，Native 用 kotlin.native.concurrent */
expect class CacheLock() {
    fun <T> withLock(block: () -> T): T
}
