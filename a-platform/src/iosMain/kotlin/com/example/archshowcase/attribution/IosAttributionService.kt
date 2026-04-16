package com.example.archshowcase.attribution

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.attribution.service.AttributionService
import com.example.archshowcase.getAttributionBridgeOrNull

class IosAttributionService : AttributionService {

    override fun initialize(appToken: String, isProduction: Boolean) {
        val bridge = getAttributionBridgeOrNull()
        if (bridge == null) {
            Log.w(TAG) { "AttributionBridge not set, skipping initialize" }
            return
        }
        bridge.initialize(appToken, isProduction)
        Log.d(TAG) { "Adjust initialized via bridge: production=$isProduction" }
    }

    override fun trackEvent(eventToken: String) {
        val bridge = getAttributionBridgeOrNull()
        if (bridge == null) {
            Log.w(TAG) { "AttributionBridge not set, skipping trackEvent" }
            return
        }
        bridge.trackEvent(eventToken)
        Log.d(TAG) { "Tracked event via bridge: $eventToken" }
    }

    companion object {
        private const val TAG = "IosAttribution"
    }
}
