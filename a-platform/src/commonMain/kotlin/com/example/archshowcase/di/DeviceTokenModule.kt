package com.example.archshowcase.di

import com.example.archshowcase.devicetoken.DeviceTokenService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val deviceTokenModule = module {
    singleOf(::DeviceTokenService)
}
