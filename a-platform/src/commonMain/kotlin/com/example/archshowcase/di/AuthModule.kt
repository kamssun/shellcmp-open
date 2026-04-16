package com.example.archshowcase.di

import com.example.archshowcase.auth.AuthService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule = module {
    singleOf(::AuthService)
}
