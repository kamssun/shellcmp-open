package com.example.archshowcase.core.trace.leak

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 内存泄漏白盒审计器（Debug-only）
 *
 * 检测项：
 * - Store 注册失衡：register 无对应 unregister
 * - PlaceholderEntry 堆积：已销毁 Component 的状态快照持续累积
 * - Scope 未取消：手动创建的 CoroutineScope 未 cancel
 */
@OptIn(ExperimentalTime::class)
object LeakAuditor {

    private const val TAG = "LeakAuditor"
    private const val AUDIT_INTERVAL_MS = 10_000L
    private const val PLACEHOLDER_RATIO_THRESHOLD = 3

    private val activeStores = mutableMapOf<String, Long>()
    private val trackedScopes = mutableMapOf<String, Job>()
    private val warnings = mutableListOf<String>()

    private var auditJob: Job? = null

    private val _summary = MutableStateFlow(LeakSummary())
    val summary: StateFlow<LeakSummary> = _summary.asStateFlow()

    /**
     * 追踪 Store 注册（ComponentExtensions 自动调用）
     */
    fun trackStoreRegister(name: String) {
        if (!AppConfig.enableRestore) return
        if (name in activeStores) {
            val warning = "Store '$name' 二次 register，上次未 unregister"
            warnings.add(warning)
            Log.w(TAG) { warning }
        }
        activeStores[name] = Clock.System.now().toEpochMilliseconds()
    }

    /**
     * 追踪 Store 注销（ComponentExtensions 自动调用）
     */
    fun trackStoreUnregister(name: String) {
        if (!AppConfig.enableRestore) return
        activeStores.remove(name)
    }

    /**
     * 追踪手动 CoroutineScope（Component 可选调用）
     */
    fun trackScope(name: String, scope: CoroutineScope) {
        if (!AppConfig.enableRestore) return
        val job = scope.coroutineContext[Job] ?: return
        trackedScopes[name] = job
    }

    /**
     * 停止追踪 CoroutineScope
     */
    fun untrackScope(name: String) {
        if (!AppConfig.enableRestore) return
        trackedScopes.remove(name)
    }

    /**
     * 启动周期审计（RootComponent init 调用）
     */
    fun start(scope: CoroutineScope) {
        if (!AppConfig.enableRestore) return
        auditJob?.cancel()
        auditJob = scope.launch {
            while (isActive) {
                delay(AUDIT_INTERVAL_MS)
                audit()
            }
        }
        Log.d(TAG) { "Auditor started" }
    }

    /**
     * 停止审计（RootComponent doOnDestroy 调用）
     */
    fun stop() {
        if (!AppConfig.enableRestore) return
        auditJob?.cancel()
        auditJob = null
        Log.d(TAG) { "Auditor stopped" }
    }

    /**
     * 清理所有追踪数据（TimeTravel cancel 时调用）
     */
    fun clear() {
        if (!AppConfig.enableRestore) return
        activeStores.clear()
        trackedScopes.clear()
        warnings.clear()
        _summary.value = LeakSummary()
    }

    private fun audit() {
        val auditInfo = RestoreRegistry.getAuditInfo()
        val leakedScopes = trackedScopes.filter { (_, job) -> job.isActive }
            .keys.toList()
        val completedScopes = trackedScopes.filter { (_, job) -> !job.isActive }
            .keys.toList()

        // 自动清理已完成的 scope
        completedScopes.forEach { trackedScopes.remove(it) }

        val currentWarnings = warnings.toList()
        val auditWarnings = mutableListOf<String>()
        auditWarnings.addAll(currentWarnings)

        if (auditInfo.activeCount > 0 &&
            auditInfo.placeholderCount > auditInfo.activeCount * PLACEHOLDER_RATIO_THRESHOLD
        ) {
            auditWarnings.add(
                "PlaceholderEntry 堆积: ${auditInfo.placeholderCount} " +
                    "(active 的 ${auditInfo.placeholderCount / auditInfo.activeCount.coerceAtLeast(1)} 倍)"
            )
        }

        _summary.value = LeakSummary(
            activeStoreCount = auditInfo.activeCount,
            placeholderCount = auditInfo.placeholderCount,
            activeScopeCount = leakedScopes.size,
            warnings = auditWarnings
        )

        if (auditWarnings.isNotEmpty()) {
            Log.w(TAG) { "=== Audit ===" }
            Log.w(TAG) { "Stores: ${auditInfo.activeCount} active, ${auditInfo.placeholderCount} placeholder" }
            Log.w(TAG) { "Scopes: ${trackedScopes.size} tracked, ${leakedScopes.size} leaked" }
            auditWarnings.forEach { warning ->
                Log.w(TAG) { "WARNING $warning" }
            }
        }
    }
}

data class LeakSummary(
    val activeStoreCount: Int = 0,
    val placeholderCount: Int = 0,
    val activeScopeCount: Int = 0,
    val warnings: List<String> = emptyList()
)
