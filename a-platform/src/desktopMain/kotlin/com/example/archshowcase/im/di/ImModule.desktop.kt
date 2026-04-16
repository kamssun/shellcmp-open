package com.example.archshowcase.im.di

import com.example.archshowcase.im.MockChatRoomService
import com.example.archshowcase.im.MockImService
import com.example.archshowcase.im.service.ChatRoomService
import com.example.archshowcase.im.service.ImService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createImPlatformModule(): Module = module {
    singleOf(::MockImService) bind ImService::class
    singleOf(::MockChatRoomService) bind ChatRoomService::class
}
