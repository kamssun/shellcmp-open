package com.example.archshowcase.presentation.timetravel

/**
 * TimeTravelComponent 持有者，供 RecordReceiver 等外部入口访问。
 * 全局持有者，供 RecordReceiver 等外部入口访问。
 */
object TimeTravelComponentHolder {
    private var _instance: TimeTravelComponent? = null

    val instance: TimeTravelComponent?
        get() = _instance

    fun set(component: TimeTravelComponent) {
        _instance = component
    }

    fun clear() {
        _instance = null
    }
}
