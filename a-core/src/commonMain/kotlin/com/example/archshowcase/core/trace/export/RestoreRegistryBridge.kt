package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.restore.RestoreRegistry

/**
 * RestoreRegistry 与 TimeTravelEvent 之间的桥接
 *
 * 将 MVIKotlin TimeTravel 事件格式的转换逻辑放在 export 包，
 * 避免 RestoreRegistry 直接依赖 TimeTravel API。
 */

/**
 * 将所有快照生成为 TimeTravelEvent 列表（用于导出）
 */
fun RestoreRegistry.generateStateEvents(
    startEventId: Long = 1L
): Pair<List<TimeTravelEvent>, Long> {
    var eventId = startEventId
    val events = getAllSnapshots().mapNotNull { (name, state) ->
        if (state.hasValidData()) {
            TimeTravelEvent(eventId++, name, StoreEventType.STATE, state, state)
        } else {
            null
        }
    }
    return events to eventId
}

/**
 * 从 TimeTravelEvent 列表中恢复快照（用于导入）
 */
fun RestoreRegistry.restoreFromEvents(events: List<TimeTravelEvent>) {
    val storeNames = events.map { it.storeName }.distinct()
    storeNames.forEach { storeName ->
        val lastState = events
            .filter { it.storeName == storeName && it.type == StoreEventType.STATE }
            .lastOrNull()
            ?.value as? RestorableState

        if (lastState?.hasValidData() == true) {
            updateSnapshotOrCreate(storeName, lastState)
        }
    }
}
