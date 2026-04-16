package com.example.archshowcase.core.trace.store

import app.cash.turbine.test
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.archshowcase.core.trace.tester.TestTraceStore
import com.example.archshowcase.core.trace.tester.TestTraceStore.Intent
import com.example.archshowcase.core.trace.tester.TestTraceStore.Label
import com.example.archshowcase.core.trace.tester.TestTraceStore.UserAction
import com.example.archshowcase.core.trace.tester.TestTraceStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TraceStoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storeFactory: StoreFactory
    private lateinit var store: TestTraceStore

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        storeFactory = DefaultStoreFactory()
        store = TestTraceStoreFactory(storeFactory).create()
    }

    @AfterTest
    fun teardown() {
        store.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = store.state
        assertTrue(state.actions.isEmpty())
        assertNull(state.crashSnapshot)
        assertFalse(state.isRestored)
        assertNull(state.importError)
    }

    @Test
    fun `RecordAction adds action to state`() {
        val action = createAction(1, "点击登录")

        store.accept(Intent.RecordAction(action))

        val state = store.state
        assertEquals(1, state.actions.size)
        assertEquals(action, state.actions.first())
        assertTrue(state.statusMessage.contains("1"))
    }

    @Test
    fun `multiple RecordAction accumulates actions`() {
        val actions = listOf(
            createAction(1, "打开应用"),
            createAction(2, "点击首页"),
            createAction(3, "浏览商品")
        )

        actions.forEach { store.accept(Intent.RecordAction(it)) }

        val state = store.state
        assertEquals(3, state.actions.size)
        assertEquals(actions, state.actions)
    }

    @Test
    fun `TriggerCrash creates snapshot and clears actions`() = runTest {
        val actions = listOf(
            createAction(1, "操作1"),
            createAction(2, "操作2")
        )
        actions.forEach { store.accept(Intent.RecordAction(it)) }

        store.accept(Intent.TriggerCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertTrue(state.actions.isEmpty())
        val snapshot = assertNotNull(state.crashSnapshot)
        assertEquals(2, snapshot.actions.size)
        assertFalse(state.isRestored)
    }

    @Test
    fun `TriggerCrash does nothing when no actions`() = runTest {
        store.accept(Intent.TriggerCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertNull(state.crashSnapshot)
    }

    @Test
    fun `RestoreFromCrash restores actions from snapshot`() = runTest {
        val actions = listOf(
            createAction(1, "登录"),
            createAction(2, "下单")
        )
        actions.forEach { store.accept(Intent.RecordAction(it)) }

        store.accept(Intent.TriggerCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.RestoreFromCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertEquals(2, state.actions.size)
        assertTrue(state.isRestored)
    }

    @Test
    fun `RestoreFromCrash does nothing when no snapshot`() = runTest {
        store.accept(Intent.RestoreFromCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertTrue(state.actions.isEmpty())
        assertFalse(state.isRestored)
    }

    @Test
    fun `ClearHistory resets all state`() = runTest {
        val actions = listOf(createAction(1, "测试"))
        actions.forEach { store.accept(Intent.RecordAction(it)) }

        store.accept(Intent.TriggerCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        store.accept(Intent.ClearHistory)

        val state = store.state
        assertTrue(state.actions.isEmpty())
        assertNull(state.crashSnapshot)
        assertFalse(state.isRestored)
    }

    @Test
    fun `ImportActions parses valid JSON`() = runTest {
        val json = """
            {
                "version": 1,
                "exportTime": 1700000000000,
                "actions": [
                    {"id": 1, "name": "导入动作1", "timestamp": 1699999999000},
                    {"id": 2, "name": "导入动作2", "timestamp": 1699999999100}
                ]
            }
        """.trimIndent()

        store.accept(Intent.ImportActions(json))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertEquals(2, state.actions.size)
        assertEquals("导入动作1", state.actions[0].name)
        assertEquals("导入动作2", state.actions[1].name)
        assertNull(state.importError)
    }

    @Test
    fun `ImportActions fails with invalid JSON`() = runTest {
        store.accept(Intent.ImportActions("invalid json"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        assertTrue(state.actions.isEmpty())
        assertNotNull(state.importError)
    }

    @Test
    fun `ImportActions fails with unsupported version`() = runTest {
        val json = """
            {
                "version": 99,
                "exportTime": 1700000000000,
                "actions": []
            }
        """.trimIndent()

        store.accept(Intent.ImportActions(json))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = store.state
        val error = assertNotNull(state.importError)
        assertTrue(error.contains("不支持的版本"))
    }

    @Test
    fun `ImportActionsList directly imports actions list`() {
        val actions = listOf(
            createAction(1, "直接导入1"),
            createAction(2, "直接导入2")
        )

        store.accept(Intent.ImportActionsList(actions))

        val state = store.state
        assertEquals(2, state.actions.size)
        assertEquals(actions, state.actions)
    }

    @Test
    fun `SetImportError sets error message`() {
        store.accept(Intent.SetImportError("自定义错误"))

        val state = store.state
        assertEquals("自定义错误", state.importError)
    }

    @Test
    fun `ClearImportError clears error`() {
        store.accept(Intent.SetImportError("错误"))
        store.accept(Intent.ClearImportError)

        val state = store.state
        assertNull(state.importError)
    }

    @Test
    fun `TriggerCrash publishes CrashOccurred label`() = runTest {
        store.accept(Intent.RecordAction(createAction(1, "测试")))

        store.labels.test {
            store.accept(Intent.TriggerCrash)
            testDispatcher.scheduler.advanceUntilIdle()

            val label = awaitItem()
            val crashLabel = assertIs<Label.CrashOccurred>(label)
            assertEquals(1, crashLabel.snapshot.actions.size)
        }
    }

    @Test
    fun `RestoreFromCrash publishes Restored label`() = runTest {
        store.accept(Intent.RecordAction(createAction(1, "测试")))
        store.accept(Intent.TriggerCrash)
        testDispatcher.scheduler.advanceUntilIdle()

        store.labels.test {
            store.accept(Intent.RestoreFromCrash)
            testDispatcher.scheduler.advanceUntilIdle()

            val label = awaitItem()
            assertTrue(label is com.example.archshowcase.core.trace.tester.TestTraceStore.Label.Restored)
        }
    }

    private fun createAction(id: Int, name: String) = UserAction(
        id = id,
        name = name,
        timestamp = Clock.System.now().toEpochMilliseconds()
    )
}
