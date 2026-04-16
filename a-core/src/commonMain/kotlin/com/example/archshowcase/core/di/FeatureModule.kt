package com.example.archshowcase.core.di

import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

/**
 * 轻量 Feature 模块模板：统一注册与加载
 */
class FeatureModule<T : Any>(
    val module: Module,
    private val loader: () -> Unit
) {
    fun load() = loader()
}

inline fun <reified T : Any> featureModuleOf(
    noinline register: Module.() -> Unit
): FeatureModule<T> {
    val featureModule = module { register() }
    return FeatureModule(featureModule) { loadModuleOnce<T>(featureModule) }
}

/** 动态加载模块（防止重复加载） */
@PublishedApi
internal inline fun <reified T : Any> loadModuleOnce(module: Module) {
    if (getKoin().getOrNull<T>() == null) {
        loadKoinModules(module)
    }
}
