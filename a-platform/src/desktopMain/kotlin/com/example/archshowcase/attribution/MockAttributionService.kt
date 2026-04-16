package com.example.archshowcase.attribution

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.attribution.service.AttributionService

class MockAttributionService : AttributionService {

    override fun initialize(appToken: String, isProduction: Boolean) {
        Log.d(TAG) { "Mock attribution initialized: production=$isProduction" }
    }

    override fun trackEvent(eventToken: String) {
        Log.d(TAG) { "Mock tracked event: $eventToken" }
    }

    companion object {
        private const val TAG = "MockAttribution"
    }
}
