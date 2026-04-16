package com.example.archshowcase.core.analytics

import com.example.archshowcase.core.analytics.model.NavigationAction

/** 保存当前导航动作类型，由 NavigationStackManager 写入，PerfMonitor.trackPage 读取 */
object NavigationActionContext {
    // public var：NavigationStackManager（a-shared）跨模块写入，无法 internal set
    var current: NavigationAction = NavigationAction.UNKNOWN
}
