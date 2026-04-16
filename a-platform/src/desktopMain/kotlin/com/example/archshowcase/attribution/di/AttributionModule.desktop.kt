package com.example.archshowcase.attribution.di

import com.example.archshowcase.attribution.MockAttributionService
import com.example.archshowcase.attribution.service.AttributionService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createAttributionPlatformModule(): Module = module {
    singleOf(::MockAttributionService) bind AttributionService::class
}
