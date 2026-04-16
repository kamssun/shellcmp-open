package com.example.archshowcase.presentation.demo.network

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val networkDemoFeature = featureModuleOf<NetworkDemoStoreFactory> {
    singleOf(::NetworkDemoStoreFactory)
}

fun loadNetworkDemoModule() = networkDemoFeature.load()
