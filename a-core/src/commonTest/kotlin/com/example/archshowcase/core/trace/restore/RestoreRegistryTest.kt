package com.example.archshowcase.core.trace.restore

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class RegTestState(
    val value: String = "",
    val count: Int = 0
) : RestorableState {
    override fun hasValidData() = value.isNotEmpty()
}

private sealed interface RegTestIntent : JvmSerializable {
    data class SetValue(val v: String) : RegTestIntent
}

private sealed interface RegTestMsg : JvmSerializable {
    data class ValueSet(val v: String) : RegTestMsg
}

@OptIn(ExperimentalCoroutinesApi::class)
class RestoreRegistryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val storeFactory: StoreFactory = DefaultStoreFactory()

    private fun createStore(initial: RegTestState = RegTestState()): Store<RegTestIntent, RegTestState, Nothing> =
        storeFactory.create(
            name = "RegTestStore",
            initialState = initial,
            executorFactory = {
                object : CoroutineExecutor<RegTestIntent, Nothing, RegTestState, RegTestMsg, Nothing>() {
                    override fun executeIntent(intent: RegTestIntent) {
                        when (intent) {
                            is RegTestIntent.SetValue -> dispatch(RegTestMsg.ValueSet(intent.v))
                        }
                    }
                }
            },
            reducer = object : Reducer<RegTestState, RegTestMsg> {
                override fun RegTestState.reduce(msg: RegTestMsg): RegTestState = when (msg) {
                    is RegTestMsg.ValueSet -> copy(value = msg.v, count = count + 1)
                }
            }
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AppConfig.enableRestore = true
        RestoreRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        RestoreRegistry.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `register returns noop disposable when enableRestore is false`() {
        AppConfig.enableRestore = false
        val store = createStore()
        val scope = TestScope(testDispatcher)

        val disposable = RestoreRegistry.register("test", store, scope)
        disposable.dispose()

        assertFalse(RestoreRegistry.wasEverRegistered("test"))
        store.dispose()
    }

    @Test
    fun `register adds store and tracks it`() = runTest {
        val store = createStore(RegTestState("initial"))
        val scope = TestScope(testDispatcher)

        val disposable = RestoreRegistry.register("myStore", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(RestoreRegistry.wasEverRegistered("myStore"))
        assertNotNull(RestoreRegistry.getSnapshot<RegTestState>("myStore"))

        disposable.dispose()
        store.dispose()
    }

    @Test
    fun `unregister with valid data keeps placeholder`() = runTest {
        val store = createStore(RegTestState("data"))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("store", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        RestoreRegistry.unregister("store")

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("store")
        assertNotNull(snapshot)
        assertEquals("data", snapshot.value)

        store.dispose()
    }

    @Test
    fun `unregister with invalid data removes entry`() = runTest {
        val store = createStore(RegTestState(""))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("store", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        RestoreRegistry.unregister("store")

        assertNull(RestoreRegistry.getSnapshot<RegTestState>("store"))

        store.dispose()
    }

    @Test
    fun `unregister unknown name is noop`() {
        RestoreRegistry.unregister("nonexistent")
    }

    @Test
    fun `getAllSnapshots returns all entries`() {
        RestoreRegistry.updateSnapshotOrCreate("a", RegTestState("v1"))
        RestoreRegistry.updateSnapshotOrCreate("b", RegTestState("v2"))

        val snapshots = RestoreRegistry.getAllSnapshots()
        assertEquals(2, snapshots.size)
        assertEquals(RegTestState("v1"), snapshots["a"])
        assertEquals(RegTestState("v2"), snapshots["b"])
    }

    @Test
    fun `updateSnapshotOrCreate creates new placeholder`() {
        RestoreRegistry.updateSnapshotOrCreate("new", RegTestState("created"))

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("new")
        assertEquals(RegTestState("created"), snapshot)
    }

    @Test
    fun `updateSnapshotOrCreate updates existing entry`() {
        RestoreRegistry.updateSnapshotOrCreate("store", RegTestState("v1"))
        RestoreRegistry.updateSnapshotOrCreate("store", RegTestState("v2"))

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("store")
        assertEquals(RegTestState("v2"), snapshot)
    }

    @Test
    fun `getSnapshot returns null for unknown name`() {
        assertNull(RestoreRegistry.getSnapshot<RegTestState>("unknown"))
    }

    @Test
    fun `wasEverRegistered returns false for never registered`() {
        assertFalse(RestoreRegistry.wasEverRegistered("nope"))
    }

    @Test
    fun `wasEverRegistered returns true even after unregister`() = runTest {
        val store = createStore(RegTestState("data"))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("persistent", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()
        RestoreRegistry.unregister("persistent")

        assertTrue(RestoreRegistry.wasEverRegistered("persistent"))

        store.dispose()
    }

    @Test
    fun `getAuditInfo returns zeros when enableRestore is false`() {
        AppConfig.enableRestore = false
        val info = RestoreRegistry.getAuditInfo()
        assertEquals(0, info.activeCount)
        assertEquals(0, info.placeholderCount)
        assertTrue(info.names.isEmpty())
    }

    @Test
    fun `getAuditInfo counts active and placeholder entries`() = runTest {
        val store = createStore(RegTestState("active"))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("active", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        RestoreRegistry.updateSnapshotOrCreate("placeholder", RegTestState("cached"))

        val info = RestoreRegistry.getAuditInfo()
        assertEquals(1, info.activeCount)
        assertEquals(1, info.placeholderCount)
        assertEquals(2, info.names.size)

        store.dispose()
    }

    @Test
    fun `clear removes all entries and resets everRegistered`() = runTest {
        val store = createStore(RegTestState("data"))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("store", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        RestoreRegistry.clear()

        assertFalse(RestoreRegistry.wasEverRegistered("store"))
        assertTrue(RestoreRegistry.getAllSnapshots().isEmpty())

        store.dispose()
    }

    @Test
    fun `register syncs state changes`() = runTest {
        val store = createStore(RegTestState(""))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("sync", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(RegTestIntent.SetValue("updated"))
        testDispatcher.scheduler.advanceUntilIdle()

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("sync")
        assertEquals("updated", snapshot?.value)

        store.dispose()
    }

    @Test
    fun `register preserves existing snapshot`() = runTest {
        RestoreRegistry.updateSnapshotOrCreate("cached", RegTestState("old"))

        val store = createStore(RegTestState("new"))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("cached", store, scope)

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("cached")
        assertEquals("old", snapshot?.value)

        store.dispose()
    }

    @Test
    fun `updateSnapshotOrCreate on registered live store updates its snapshot`() = runTest {
        val store = createStore(RegTestState("live"))
        val scope = TestScope(testDispatcher)

        RestoreRegistry.register("liveStore", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        // updateSnapshotOrCreate on a live StoreEntry
        RestoreRegistry.updateSnapshotOrCreate("liveStore", RegTestState("updated"))

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("liveStore")
        assertEquals("updated", snapshot?.value)

        store.dispose()
    }

    @Test
    fun `Disposable dispose unregisters store`() = runTest {
        val store = createStore(RegTestState("data"))
        val scope = TestScope(testDispatcher)

        val disposable = RestoreRegistry.register("disposable", store, scope)
        testDispatcher.scheduler.advanceUntilIdle()

        disposable.dispose()

        val snapshot = RestoreRegistry.getSnapshot<RegTestState>("disposable")
        assertNotNull(snapshot)

        store.dispose()
    }

    @Test
    fun `AuditInfo data class`() {
        val info = AuditInfo(3, 2, listOf("a", "b", "c", "d", "e"))
        assertEquals(3, info.activeCount)
        assertEquals(2, info.placeholderCount)
        assertEquals(5, info.names.size)
    }
}
