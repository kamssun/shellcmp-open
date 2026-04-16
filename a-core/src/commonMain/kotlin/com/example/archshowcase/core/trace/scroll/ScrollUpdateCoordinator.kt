package com.example.archshowcase.core.trace.scroll

/**
 * 滚动更新协调器，用于标记用户滚动，避免 TimeTravel 恢复互相触发。
 */
class ScrollUpdateCoordinator {
    private var userScrolling = false

    fun isUserScrolling(): Boolean = userScrolling

    fun runWithUserScroll(block: () -> Unit) {
        userScrolling = true
        try {
            block()
        } finally {
            userScrolling = false
        }
    }
}
