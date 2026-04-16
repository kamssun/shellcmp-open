package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import com.example.archshowcase.core.compose.exposure.ExposureTracker
import kotlinx.coroutines.CoroutineScope
import kotlin.concurrent.Volatile

/**
 * Analytics 模块统一初始化入口。
 * RootComponent 只需调用 start()，不内联初始化逻辑。
 */
object AnalyticsSetup {

    @Volatile
    private var started = false

    fun start(
        scope: CoroutineScope,
        config: AnalyticsConfig = AnalyticsConfig(),
        eventMapper: ((storeName: String, intent: Any) -> TrackingEvent?)? = null,
        paramsExtractor: ((Any) -> Map<String, String>)? = null,
    ) {
        if (started) return
        started = true
        AnalyticsCollector.start(config)
        AnalyticsCollector.eventMapper = eventMapper
        ExposureTracker.init(scope)
        ExposureTracker.paramsExtractor = paramsExtractor
        ExposureTracker.onExposure = { event ->
            AnalyticsCollector.collect(
                AnalyticsEvent.Exposure(
                    route = AnalyticsCollector.currentRoute,
                    componentType = event.componentType,
                    exposureKey = event.exposureKey,
                    listId = event.listId,
                    params = event.params,
                )
            )
        }
        AppLifecycleTracker.start()
    }

    fun stop() {
        if (!started) return
        started = false
        AppLifecycleTracker.stop()
        ExposureTracker.reset()
        AnalyticsCollector.flush()
        AnalyticsCollector.reset()
    }
}
