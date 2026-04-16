package com.example.archshowcase.presentation.login

import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.auth.DefaultLoginRepository
import com.example.archshowcase.auth.LoginRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

private val loginFeature = featureModuleOf<LoginStoreFactory> {
    singleOf(::DefaultLoginRepository) bind LoginRepository::class
    singleOf(::LoginStoreFactory)
}

fun loadLoginModule() = loginFeature.load()
