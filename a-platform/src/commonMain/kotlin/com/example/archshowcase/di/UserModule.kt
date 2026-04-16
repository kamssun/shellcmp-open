package com.example.archshowcase.di

import com.example.archshowcase.user.UserService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val userModule = module {
    singleOf(::UserService)
}
