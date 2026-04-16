// Stub implementation — SDK dependencies removed for open-source showcase
package com.example.archshowcase.attribution

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.attribution.service.AttributionService
import java.util.concurrent.atomic.AtomicBoolean

class AndroidAttributionService : AttributionService {

    private val initialized = AtomicBoolean(false)

    override fun initialize(appToken: String, isProduction: Boolean) {
        if (!initialized.compareAndSet(false, true)) return
        // Stub: Attribution SDK not available
        Log.d(TAG) { "Stub: Attribution SDK not initialized (production=$isProduction)" }
    }

    override fun trackEvent(eventToken: String) {
        if (!initialized.get()) {
            Log.w(TAG) { "trackEvent called before initialize" }
            return
        }
        // Stub: event not tracked
        Log.d(TAG) { "Stub: trackEvent (no-op, token=$eventToken)" }
    }

    companion object {
        private const val TAG = "AndroidAttribution"
    }
}
