package com.example.archshowcase.presentation.demo.obo

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val oboDemoFeature = featureModuleOf<OBODemoStoreFactory> {
    singleOf(::OBODemoStoreFactory)
}

fun loadOBODemoModule() = oboDemoFeature.load()
