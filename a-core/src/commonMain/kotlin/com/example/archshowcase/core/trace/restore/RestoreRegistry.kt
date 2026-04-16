package com.example.archshowcase.core.trace.restore

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * 全局状态恢复注册中心
 *
 * 职责：
 * 1. 注册/注销可恢复的 Store
 * 2. 自动同步状态到快照
 * 3. 快照的读写访问
 *
 * 初始状态解析由 [InitialStateResolver] 负责。
 * TimeTravel 事件桥接由 export 包的扩展函数负责。
 */
object RestoreRegistry {

    private val entries = mutableMapOf<String, Entry>()
    private val everRegistered = mutableSetOf<String>()

    /**
     * 注册 Store
     *
     * @param name Store 名称（用于 TimeTravel 匹配）
     * @param store MVIKotlin Store 实例
     * @param scope 用于状态同步的协程作用域
     * @return Disposable 用于注销
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <S : RestorableState> register(
        name: String,
        store: Store<*, S, *>,
        scope: CoroutineScope
    ): Disposable {
        if (!AppConfig.enableRestore) return Disposable {}
        val existingSnapshot = entries[name]?.currentSnapshot
        val entry = StoreEntry(store, scope, existingSnapshot)
        entries[name] = entry
        everRegistered.add(name)
        entry.startSync()
        return Disposable { unregister(name) }
    }

    fun unregister(name: String) {
        val entry = entries[name] ?: return
        entry.stopSync()
        if (entry.currentSnapshot.hasValidData()) {
            entries[name] = PlaceholderEntry(entry.currentSnapshot)
        } else {
            entries.remove(name)
        }
    }

    /**
     * 获取所有已注册 Store 的当前状态快照
     */
    fun getAllSnapshots(): Map<String, RestorableState> {
        return entries.mapValues { it.value.currentSnapshot }
    }

    /**
     * 清空所有快照（验证模式初始化时使用，确保 Store 从干净状态开始）
     */
    fun clearAllSnapshots() {
        entries.values.forEach { it.stopSync() }
        entries.clear()
    }

    /**
     * 更新或创建快照（用于导入恢复）
     */
    fun updateSnapshotOrCreate(name: String, state: RestorableState) {
        val entry = entries[name]
        if (entry != null) {
            entry.updateSnapshot(state)
        } else {
            entries[name] = PlaceholderEntry(state)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : RestorableState> getSnapshot(name: String): S? {
        return entries[name]?.currentSnapshot as? S
    }

    /**
     * 该 Store 是否曾经注册过（用于 InitialStateResolver 判断策略）
     */
    fun wasEverRegistered(storeName: String): Boolean = storeName in everRegistered

    /**
     * 解析 Store 的初始状态，委托给 [InitialStateResolver]
     */
    fun <S : RestorableState> resolveInitialState(storeName: String): S? =
        InitialStateResolver.resolve(storeName)

    /**
     * 查找运行时 Store 引用（Phase 2: ADB 验证用）
     *
     * 仅从活跃的 StoreEntry 获取，PlaceholderEntry 返回 null。
     */
    @Suppress("UNCHECKED_CAST")
    fun findStore(name: String): Store<*, *, *>? {
        return (entries[name] as? StoreEntry<*>)?.store
    }

    /**
     * 获取审计信息（LeakAuditor 调用）
     */
    fun getAuditInfo(): AuditInfo {
        if (!AppConfig.enableRestore) return AuditInfo(0, 0, emptyList())
        var active = 0
        var placeholder = 0
        entries.forEach { (_, v) -> if (v is PlaceholderEntry) placeholder++ else active++ }
        return AuditInfo(active, placeholder, entries.keys.toList())
    }

    /**
     * 清理所有状态（TimeTravel 取消时调用）
     */
    fun clear() {
        entries.values.forEach { it.stopSync() }
        entries.clear()
        everRegistered.clear()
    }

    private interface Entry {
        val currentSnapshot: RestorableState
        fun stopSync()
        fun updateSnapshot(state: RestorableState)
    }

    private class StoreEntry<S : RestorableState>(
        val store: Store<*, S, *>,
        val scope: CoroutineScope,
        cachedSnapshot: RestorableState? = null
    ) : Entry {
        @Volatile
        override var currentSnapshot: RestorableState = cachedSnapshot ?: store.state
            private set

        private var syncJob: Job? = null

        @OptIn(ExperimentalCoroutinesApi::class)
        fun startSync() {
            syncJob = scope.launch {
                store.stateFlow.collect { state ->
                    if (state.hasValidData()) {
                        currentSnapshot = state
                    }
                }
            }
        }

        override fun stopSync() {
            syncJob?.cancel()
        }

        override fun updateSnapshot(state: RestorableState) {
            currentSnapshot = state
        }
    }

    private class PlaceholderEntry(
        override var currentSnapshot: RestorableState
    ) : Entry {
        override fun stopSync() {}
        override fun updateSnapshot(state: RestorableState) {
            currentSnapshot = state
        }
    }
}

data class AuditInfo(val activeCount: Int, val placeholderCount: Int, val names: List<String>)

fun interface Disposable {
    fun dispose()
}
