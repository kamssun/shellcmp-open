package com.example.archshowcase.im.di

import com.example.archshowcase.im.IosChatRoomService
import com.example.archshowcase.im.IosImService
import com.example.archshowcase.im.service.ChatRoomService
import com.example.archshowcase.im.service.ImService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createImPlatformModule(): Module = module {
    singleOf(::IosImService) bind ImService::class
    singleOf(::IosChatRoomService) bind ChatRoomService::class
}
