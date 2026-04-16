// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.auth

import android.content.Context
import com.example.archshowcase.core.util.Log
import java.util.concurrent.atomic.AtomicBoolean

data class AuthInitializerConfig(
    val serverUrl: String,
    val appKey: String,
    val deviceId: String,
    val googleServerClientId: String,
    val enableLogging: Boolean
)

object AuthInitializer {
    private const val TAG = "AuthInitializer"

    private val initialized = AtomicBoolean(false)

    fun initialize(context: Context, config: AuthInitializerConfig) {
        if (initialized.getAndSet(true)) return

        if (config.serverUrl.isBlank()) {
            Log.w(TAG) { "serverUrl is empty, skipping initialization" }
            return
        }

        if (config.appKey.isBlank()) {
            Log.w(TAG) { "appKey is empty" }
        }

        initAuthSdk(context.applicationContext, config)
        initLoginSdk(context.applicationContext, config)

        AuthService.refreshStateFromSdk()
    }

    private fun initAuthSdk(context: Context, config: AuthInitializerConfig) {
        // Stub: Auth SDK not initialized
        Log.d(TAG) { "Stub: Auth SDK not initialized" }
    }

    private fun initLoginSdk(context: Context, config: AuthInitializerConfig) {
        // Stub: Login SDK not initialized
        Log.d(TAG) { "Stub: Login SDK not initialized" }
    }
}
