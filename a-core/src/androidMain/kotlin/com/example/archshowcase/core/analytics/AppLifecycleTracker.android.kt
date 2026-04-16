package com.example.archshowcase.core.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

actual object AppLifecycleTracker {

    @Volatile
    private var started = false

    private val observer = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            onAppBackground()
        }

        override fun onStart(owner: LifecycleOwner) {
            onAppForeground()
        }
    }

    actual fun start() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    actual fun stop() {
        if (!started) return
        started = false
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
    }
}
