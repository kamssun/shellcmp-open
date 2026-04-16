package com.example.archshowcase.core.trace.user

import com.arkivanov.mvikotlin.core.store.Bootstrapper
import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.example.archshowcase.core.analytics.AnalyticsCollector
import com.example.archshowcase.core.analytics.InteractionContext
import com.example.archshowcase.core.analytics.model.AnalyticsEvent

class IntentTrackingStoreFactory(
    private val delegate: StoreFactory
) : StoreFactory {

    override fun <Intent : Any, Action : Any, Message : Any, State : Any, Label : Any> create(
        name: String?,
        autoInit: Boolean,
        initialState: State,
        bootstrapper: Bootstrapper<Action>?,
        executorFactory: () -> Executor<Intent, Action, State, Message, Label>,
        reducer: Reducer<State, Message>
    ): Store<Intent, State, Label> {
        val storeName = name ?: "Unknown"
        val wrappedExecutorFactory: () -> Executor<Intent, Action, State, Message, Label> = {
            TrackingExecutor(storeName, executorFactory())
        }

        return delegate.create(
            name = name,
            autoInit = autoInit,
            initialState = initialState,
            bootstrapper = bootstrapper,
            executorFactory = wrappedExecutorFactory,
            reducer = reducer
        )
    }
}

private class TrackingExecutor<Intent : Any, Action : Any, State : Any, Message : Any, Label : Any>(
    private val storeName: String,
    private val delegate: Executor<Intent, Action, State, Message, Label>
) : Executor<Intent, Action, State, Message, Label> {

    override fun init(callbacks: Executor.Callbacks<State, Message, Action, Label>) {
        delegate.init(callbacks)
    }

    override fun executeIntent(intent: Intent) {
        IntentTracker.record(storeName, intent)
        trackUserIntent(intent)
        delegate.executeIntent(intent)
    }

    /**
     * 仅当 Intent 由用户手势触发时，自动采集埋点事件。
     *
     * 判定流程：
     * 1. 读取 InteractionContext —— 无手势上下文则跳过（系统行为，如 Flow/init 触发）
     * 2. 调用 KSP 生成的 EventMapper —— 将 Intent 转为 TrackingEvent（事件名 + 参数）
     * 3. RuntimeSanitizer 脱敏 —— 截断长字符串、mask 手机号/邮箱
     * 4. 提交到 AnalyticsCollector
     */
    private fun trackUserIntent(intent: Intent) {
        val gesture = InteractionContext.currentGesture() ?: return
        val trackingEvent = AnalyticsCollector.eventMapper?.invoke(storeName, intent) ?: return
        AnalyticsCollector.collect(
            AnalyticsEvent.Intent(
                route = AnalyticsCollector.currentRoute,
                storeName = storeName,
                intentName = trackingEvent.name.substringAfter('.'),
                gestureType = gesture.gestureType,
                params = trackingEvent.params,
            )
        )
    }

    override fun executeAction(action: Action) {
        delegate.executeAction(action)
    }

    override fun dispose() {
        delegate.dispose()
    }
}
