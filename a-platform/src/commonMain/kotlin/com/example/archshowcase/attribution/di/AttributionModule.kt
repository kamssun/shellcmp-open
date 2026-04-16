package com.example.archshowcase.attribution.di

import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.attribution.service.AttributionService
import org.koin.core.module.Module

expect fun createAttributionPlatformModule(): Module

private val attributionFeature = featureModuleOf<AttributionService> {
    includes(createAttributionPlatformModule())
}

fun loadAttributionModule() = attributionFeature.load()
