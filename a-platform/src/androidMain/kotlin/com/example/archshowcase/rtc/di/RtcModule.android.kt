package com.example.archshowcase.rtc.di

import com.example.archshowcase.rtc.AndroidRtcService
import com.example.archshowcase.rtc.service.RtcService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createRtcPlatformModule(): Module = module {
    singleOf(::AndroidRtcService) bind RtcService::class
}
