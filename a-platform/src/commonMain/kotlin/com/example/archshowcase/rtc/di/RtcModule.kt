package com.example.archshowcase.rtc.di

import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.rtc.service.RtcService
import org.koin.core.module.Module

expect fun createRtcPlatformModule(): Module

private val rtcFeature = featureModuleOf<RtcService> {
    includes(createRtcPlatformModule())
}

fun loadRtcModule() = rtcFeature.load()
