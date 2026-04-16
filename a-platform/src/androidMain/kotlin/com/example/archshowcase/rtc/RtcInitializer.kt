// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.rtc

import com.example.archshowcase.core.util.Log

/**
 * Android RTC SDK initialization entry point, three phases: preInit -> init -> login
 */
object RtcInitializer {

    private const val TAG = "RtcInitializer"

    fun preInitialize() {
        // Stub: RTC SDK not available
        Log.d(TAG) { "Stub: preInitialize (no-op)" }
    }

    fun initialize(appId: String, isDebug: Boolean) {
        // Stub: RTC SDK not available
        Log.d(TAG) { "Stub: initialize (no-op, appId=${appId.take(6)}...)" }
    }

    fun login(uid: String) {
        // Stub: RTC SDK not available
        Log.d(TAG) { "Stub: login (no-op, uid=$uid)" }
    }
}
