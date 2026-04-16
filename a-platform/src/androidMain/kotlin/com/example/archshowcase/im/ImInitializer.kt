// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.im

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.im.service.ImService as AppImService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean

data class ImInitializerConfig(
    val isDebug: Boolean,
    val apiKey: String,
    val codeTag: String,
    val xlogKey: String
)

object ImInitializer : KoinComponent {
    private const val TAG = "ImInitializer"

    private val preInitDone = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val imService: AppImService by inject()

    /**
     * Phase 1: Pre-initialization (called from Application.onCreate).
     * Configures SDK parameters only, no login.
     */
    fun preInit(config: ImInitializerConfig) {
        if (preInitDone.getAndSet(true)) return

        if (config.apiKey.isBlank()) {
            Log.w(TAG) { "IM SDK apiKey is empty, skipping preInit" }
            return
        }

        // Stub: IM SDK not initialized
        Log.d(TAG) { "Stub: IM SDK preInit (no-op)" }
    }

    /**
     * Phase 2: Initialize and login (called after auth login succeeds).
     * Sets user info, initializes, logs in, and sets up auto-reconnect.
     */
    fun initializeAndLogin(memberId: String, nickName: String) {
        if (initialized.getAndSet(true)) return

        // Stub: IM SDK not available
        Log.d(TAG) { "Stub: IM SDK initializeAndLogin (no-op, memberId=$memberId)" }
    }

    /**
     * Logout cleanup.
     */
    fun logout() {
        initialized.set(false)
        Log.d(TAG) { "Stub: IM SDK logged out" }
    }
}
