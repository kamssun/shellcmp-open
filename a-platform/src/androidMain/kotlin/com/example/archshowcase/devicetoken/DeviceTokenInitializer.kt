// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.devicetoken

import android.app.Application
import android.content.Context
import com.example.archshowcase.core.util.ContextProvider
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.network.header.HeaderConstants
import java.util.concurrent.atomic.AtomicBoolean

data class DeviceTokenInitializerConfig(
    val aliKey: String,
    val signKey: String = "",
    val signKeyNew: String = "",
    val signVersion: Int = 3,
    val signVersionNew: Int = 4,
    val aliyunOptions: Map<String, String> = emptyMap()
)

/** Device token initialization entry point (device fingerprint + request signing) */
object DeviceTokenInitializer {
    private const val TAG = "DeviceTokenInit"
    private const val UMID_PREF_KEY = "com.example.archshowcase.umid"

    private val initialized = AtomicBoolean(false)

    @Volatile
    private var cachedUmid: String = ""

    fun initialize(config: DeviceTokenInitializerConfig) {
        if (initialized.getAndSet(true)) return

        val app = ContextProvider.applicationContext as? Application ?: run {
            Log.w(TAG) { "applicationContext is not Application, skipping init" }
            return
        }

        if (config.aliKey.isBlank()) {
            Log.w(TAG) { "aliKey is empty, skipping SDK init" }
            return
        }

        // Restore cached umid from preferences
        val prefs = app.getSharedPreferences("archshowcase_device", Context.MODE_PRIVATE)
        val savedUmid = prefs.getString(UMID_PREF_KEY, null).orEmpty()
        if (savedUmid.isNotBlank()) {
            cachedUmid = savedUmid
            HeaderConstants.currentUmid = savedUmid
        }

        // Stub: Device token SDK not initialized
        Log.d(TAG) { "Stub: Device token SDK not initialized" }
    }

    fun getUmid(): String? = cachedUmid.ifBlank { null }

    fun sign(fields: Map<String, String>): String? {
        // Stub: signing not available
        return null
    }

    fun reportAction(action: DeviceTokenAction) {
        // Stub: action reporting not available
        Log.d(TAG) { "Stub: reportAction (no-op, action=$action)" }
    }
}
