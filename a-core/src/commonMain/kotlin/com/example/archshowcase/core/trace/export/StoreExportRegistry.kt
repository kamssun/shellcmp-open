package com.example.archshowcase.core.trace.export

/**
 * Store 导出策略注册表
 */
object StoreExportRegistry {
    private val strategies = mutableMapOf<String, StoreExportStrategy>()
    private var externalRegistrar: (() -> Unit)? = null

    fun setExternalRegistrar(registrar: () -> Unit) {
        externalRegistrar = registrar
    }

    fun register(strategy: StoreExportStrategy) {
        strategies[strategy.storeName] = strategy
    }

    fun get(storeName: String): StoreExportStrategy? {
        strategies[storeName]?.let { return it }
        // 前缀匹配：实例化 store name（如 "ChatRoomStore:convA"）匹配基础策略 "ChatRoomStore"
        val colonIndex = storeName.indexOf(':')
        if (colonIndex > 0) {
            return strategies[storeName.substring(0, colonIndex)]
        }
        return null
    }

    fun getAll(): Collection<StoreExportStrategy> = strategies.values

    fun getStoreNames(): Set<String> = strategies.keys.toSet()

    fun runExternalRegistrar() {
        externalRegistrar?.invoke()
    }

    fun clear() {
        strategies.clear()
    }
}
