package com.example.archshowcase.chat.local

import platform.Foundation.NSLock

actual class CacheLock actual constructor() {
    private val lock = NSLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
