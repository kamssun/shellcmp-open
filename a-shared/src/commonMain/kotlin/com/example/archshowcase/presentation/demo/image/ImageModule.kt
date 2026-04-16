package com.example.archshowcase.presentation.demo.image

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val imageDemoFeature = featureModuleOf<ImageDemoStoreFactory> {
    singleOf(::ImageDemoStoreFactory)
}

fun loadImageModule() = imageDemoFeature.load()
