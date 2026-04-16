package com.example.archshowcase.attribution

interface AttributionBridge {
    fun initialize(appToken: String, isProduction: Boolean)
    fun trackEvent(eventToken: String)
}
