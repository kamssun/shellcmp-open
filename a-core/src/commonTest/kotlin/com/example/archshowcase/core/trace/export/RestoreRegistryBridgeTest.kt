package com.example.archshowcase.core.trace.export

import com.arkivanov.mvikotlin.core.store.StoreEventType
import com.arkivanov.mvikotlin.timetravel.TimeTravelEvent
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.trace.restore.RestorableState
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class BridgeTestState(val value: String = "") : RestorableState {
    override fun hasValidData() = value.isNotEmpty()
}

class RestoreRegistryBridgeTest {

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
        RestoreRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        RestoreRegistry.clear()
    }

    @Test
    fun `generateStateEvents returns empty for no snapshots`() {
        val (events, nextId) = RestoreRegistry.generateStateEvents()
        assertTrue(events.isEmpty())
        assertEquals(1L, nextId)
    }

    @Test
    fun `generateStateEvents creates events for valid snapshots`() {
        RestoreRegistry.updateSnapshotOrCreate("Store1", BridgeTestState("data1"))
        RestoreRegistry.updateSnapshotOrCreate("Store2", BridgeTestState("data2"))

        val (events, nextId) = RestoreRegistry.generateStateEvents()
        assertEquals(2, events.size)
        assertEquals(3L, nextId)
        assertTrue(events.all { it.type == StoreEventType.STATE })
    }

    @Test
    fun `generateStateEvents skips invalid snapshots`() {
        RestoreRegistry.updateSnapshotOrCreate("Valid", BridgeTestState("data"))
        RestoreRegistry.updateSnapshotOrCreate("Invalid", BridgeTestState(""))

        val (events, _) = RestoreRegistry.generateStateEvents()
        assertEquals(1, events.size)
        assertEquals("Valid", events.first().storeName)
    }

    @Test
    fun `generateStateEvents uses custom startEventId`() {
        RestoreRegistry.updateSnapshotOrCreate("S", BridgeTestState("x"))

        val (events, nextId) = RestoreRegistry.generateStateEvents(startEventId = 100L)
        assertEquals(100L, events.first().id)
        assertEquals(101L, nextId)
    }

    @Test
    fun `restoreFromEvents updates snapshots from STATE events`() {
        val events = listOf(
            TimeTravelEvent(1L, "StoreA", StoreEventType.STATE, BridgeTestState("v1"), BridgeTestState()),
            TimeTravelEvent(2L, "StoreA", StoreEventType.INTENT, "intent", BridgeTestState("v1")),
            TimeTravelEvent(3L, "StoreA", StoreEventType.STATE, BridgeTestState("v2"), BridgeTestState("v1")),
            TimeTravelEvent(4L, "StoreB", StoreEventType.STATE, BridgeTestState("b1"), BridgeTestState())
        )

        RestoreRegistry.restoreFromEvents(events)

        val snapshotA = RestoreRegistry.getSnapshot<BridgeTestState>("StoreA")
        assertEquals("v2", snapshotA?.value)

        val snapshotB = RestoreRegistry.getSnapshot<BridgeTestState>("StoreB")
        assertEquals("b1", snapshotB?.value)
    }

    @Test
    fun `restoreFromEvents ignores non-STATE events`() {
        val events = listOf(
            TimeTravelEvent(1L, "Store", StoreEventType.INTENT, "intent", BridgeTestState())
        )

        RestoreRegistry.restoreFromEvents(events)
        val snapshot = RestoreRegistry.getSnapshot<BridgeTestState>("Store")
        assertEquals(null, snapshot)
    }

    @Test
    fun `restoreFromEvents ignores invalid state data`() {
        val events = listOf(
            TimeTravelEvent(1L, "Store", StoreEventType.STATE, BridgeTestState(""), BridgeTestState())
        )

        RestoreRegistry.restoreFromEvents(events)
        val snapshot = RestoreRegistry.getSnapshot<BridgeTestState>("Store")
        assertEquals(null, snapshot)
    }
}
