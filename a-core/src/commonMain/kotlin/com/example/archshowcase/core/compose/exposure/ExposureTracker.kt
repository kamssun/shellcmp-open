package com.example.archshowcase.core.compose.exposure

import com.example.archshowcase.core.scheduler.OBOScheduler
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 曝光追踪器：元素可见 >= visibleAreaRatio 且停留 >= dwellTimeMs 时产生曝光事件。
 * 同一元素在同一页面生命周期内只上报一次。
 */
object ExposureTracker {

    var config = ExposureConfig()
        internal set
    var onExposure: ((ExposureEvent) -> Unit)? = null
        internal set
    /** KSP 生成的 ExposureParamsRegistry::extract，由 AnalyticsSetup 注入 */
    var paramsExtractor: ((Any) -> Map<String, String>)? = null
        internal set
    internal var dispatcher: CoroutineContext = OBOScheduler.dispatcher

    private val pendingJobs = mutableMapOf<String, Job>()
    private val exposedKeys = mutableSetOf<String>()
    /** 缓存上次可见状态（true=above threshold），状态未变时 short-circuit */
    private val lastAboveThreshold = mutableMapOf<String, Boolean>()
    private var paused = false
    private var scope: CoroutineScope? = null

    fun init(scope: CoroutineScope, config: ExposureConfig = ExposureConfig()) {
        if (this.scope != null && this.scope != scope) {
            cancelAllPending()
        }
        this.scope = scope
        this.config = config
    }

    /**
     * 由 Modifier.trackExposure 调用，报告元素可见比例变化。
     *
     * @param exposureKey 唯一标识（如 componentType + 业务 key）
     * @param componentType 组件类型
     * @param visibleRatio 当前可见比例 0.0..1.0
     * @param listId 所在列表 id
     */
    fun reportVisibility(
        exposureKey: String,
        componentType: String,
        visibleRatio: Float,
        listId: String = "",
        params: Map<String, String> = emptyMap(),
    ) {
        if (paused) return
        if (exposureKey in exposedKeys) return

        val isAbove = visibleRatio >= config.visibleAreaRatio
        val wasAbove = lastAboveThreshold[exposureKey]
        val hasPendingJob = exposureKey in pendingJobs
        if (wasAbove == isAbove && hasPendingJob == isAbove) return
        lastAboveThreshold[exposureKey] = isAbove

        if (isAbove) {
            // 达到可见阈值，启动停留计时
            if (exposureKey !in pendingJobs) {
                val s = scope ?: return
                pendingJobs[exposureKey] = s.launch(dispatcher) {
                    try {
                        delay(config.dwellTimeMs)
                        if (exposureKey !in exposedKeys && !paused) {
                            exposedKeys.add(exposureKey)
                            onExposure?.invoke(
                                ExposureEvent(
                                    componentType = componentType,
                                    exposureKey = exposureKey,
                                    listId = listId,
                                    params = params,
                                )
                            )
                        }
                    } finally {
                        pendingJobs.remove(exposureKey)
                    }
                }
            }
        } else {
            // 低于可见阈值，取消计时
            pendingJobs.remove(exposureKey)?.cancel()
        }
    }

    /** 取消指定 key 的待定曝光计时（元素离开 composition 时调用） */
    fun cancelPending(exposureKey: String) {
        pendingJobs.remove(exposureKey)?.cancel()
        lastAboveThreshold.remove(exposureKey)
    }

    /** 页面切换时重置已曝光集合 */
    fun resetPage() {
        cancelAllPending()
        exposedKeys.clear()
    }

    /** App 进入后台时暂停 */
    fun pauseAll() {
        paused = true
        cancelAllPending()
    }

    /** App 回到前台时恢复 */
    fun resumeAll() {
        paused = false
    }

    private fun cancelAllPending() {
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
        lastAboveThreshold.clear()
    }

    /** 测试用重置 */
    internal fun reset() {
        cancelAllPending()
        exposedKeys.clear()
        paused = false
        onExposure = null
        paramsExtractor = null
        scope = null
        config = ExposureConfig()
        dispatcher = OBOScheduler.dispatcher
    }
}
