package com.example.archshowcase.core.trace.tester

import com.example.archshowcase.core.di.featureModuleOf
import org.koin.core.module.dsl.singleOf

private val traceFeature = featureModuleOf<TestTraceStoreFactory> {
    singleOf(::TestTraceStoreFactory)
}

fun loadTraceModule() = traceFeature.load()
