package com.example.archshowcase.attribution.service

interface AttributionService {
    fun initialize(appToken: String, isProduction: Boolean)
    fun trackEvent(eventToken: String)
}
