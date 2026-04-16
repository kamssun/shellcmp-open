package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.AnalyticsEvent
import kotlin.random.Random

/** 按事件类型差异化采样 */
object EventSampler {

    private var random: Random = Random.Default

    fun shouldSample(event: AnalyticsEvent, config: AnalyticsConfig): Boolean {
        val rate = config.samplingRate(event)
        if (rate >= 1.0f) return true
        if (rate <= 0.0f) return false
        return random.nextFloat() < rate
    }

    /** 测试用：注入固定 Random */
    internal fun setRandom(r: Random) {
        random = r
    }

    internal fun reset() {
        random = Random.Default
    }
}
