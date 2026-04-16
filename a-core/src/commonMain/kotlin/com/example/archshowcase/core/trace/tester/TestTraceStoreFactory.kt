package com.example.archshowcase.core.trace.tester

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class TestTraceStoreFactory(
    private val storeFactory: StoreFactory
) {
    companion object Companion {
        private const val TAG = "Trace"
    }

    fun create(): TestTraceStore =
        object : TestTraceStore,
            Store<TestTraceStore.Intent, TestTraceStore.State, TestTraceStore.Label> by storeFactory.create(
                name = "TraceStore",
                initialState = TestTraceStore.State(),
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl
            ) {}

    private sealed interface Msg : JvmSerializable {
        data class ActionRecorded(val action: TestTraceStore.UserAction) : Msg
        data class CrashTriggered(val snapshot: TestTraceStore.CrashSnapshot) : Msg
        data class RestoredFromCrash(val snapshot: TestTraceStore.CrashSnapshot) : Msg
        data object HistoryCleared : Msg
        data class ImportSuccess(val actions: List<TestTraceStore.UserAction>) : Msg
        data class ImportFailed(val error: String) : Msg
        data object ClearImportError : Msg
    }

    private class ExecutorImpl : CoroutineExecutor<TestTraceStore.Intent, Nothing, TestTraceStore.State, Msg, TestTraceStore.Label>() {

        override fun executeIntent(intent: TestTraceStore.Intent) {
            when (intent) {
                is TestTraceStore.Intent.RecordAction -> recordAction(intent.action)
                is TestTraceStore.Intent.TriggerCrash -> triggerCrash()
                is TestTraceStore.Intent.RestoreFromCrash -> restoreFromCrash()
                is TestTraceStore.Intent.ClearHistory -> clearHistory()
                is TestTraceStore.Intent.ImportActions -> importActions(intent.json)
                is TestTraceStore.Intent.ImportActionsList -> importActionsList(intent.actions)
                is TestTraceStore.Intent.SetImportError -> dispatch(Msg.ImportFailed(intent.error))
                is TestTraceStore.Intent.ClearImportError -> dispatch(Msg.ClearImportError)
            }
        }

        private fun recordAction(action: TestTraceStore.UserAction) {
            Log.d(TAG) { "Recording action: ${action.name}" }
            dispatch(Msg.ActionRecorded(action))
        }

        private fun triggerCrash() {
            scope.launch {
                val currentState = state()
                if (currentState.actions.isEmpty()) {
                    Log.w(TAG) { "No actions to snapshot" }
                    return@launch
                }

                val snapshot = TestTraceStore.CrashSnapshot(
                    actions = currentState.actions,
                    crashTime = Clock.System.now().toEpochMilliseconds(),
                    errorMessage = "模拟崩溃: NullPointerException at UserAction[${currentState.actions.size}]"
                )

                Log.e(TAG) { "Crash triggered! Snapshot saved with ${snapshot.actions.size} actions" }
                dispatch(Msg.CrashTriggered(snapshot))
                publish(TestTraceStore.Label.CrashOccurred(snapshot))
            }
        }

        private fun restoreFromCrash() {
            scope.launch {
                val snapshot = state().crashSnapshot
                if (snapshot == null) {
                    Log.w(TAG) { "No crash snapshot to restore" }
                    return@launch
                }

                Log.i(TAG) { "Restoring from crash snapshot: ${snapshot.actions.size} actions" }
                dispatch(Msg.RestoredFromCrash(snapshot))
                publish(TestTraceStore.Label.Restored)
            }
        }

        private fun clearHistory() {
            Log.d(TAG) { "Clearing action history" }
            dispatch(Msg.HistoryCleared)
        }

        private fun importActions(json: String) {
            scope.launch {
                try {
                    val export = Json.decodeFromString<TestTraceStore.ActionTraceExport>(json)
                    if (export.version != 1) {
                        dispatch(Msg.ImportFailed("不支持的版本: ${export.version}"))
                        return@launch
                    }
                    Log.i(TAG) { "Imported ${export.actions.size} actions" }
                    dispatch(Msg.ImportSuccess(export.actions))
                } catch (e: Exception) {
                    Log.e(TAG) { "Import failed: ${e.message}" }
                    dispatch(Msg.ImportFailed("解析失败: ${e.message}"))
                }
            }
        }

        private fun importActionsList(actions: List<TestTraceStore.UserAction>) {
            Log.i(TAG) { "Imported ${actions.size} actions" }
            dispatch(Msg.ImportSuccess(actions))
        }
    }

    private object ReducerImpl : Reducer<TestTraceStore.State, Msg> {
        override fun TestTraceStore.State.reduce(msg: Msg): TestTraceStore.State =
            when (msg) {
                is Msg.ActionRecorded -> copy(
                    actions = actions + msg.action,
                    statusMessage = "已记录 ${actions.size + 1} 个行为"
                )
                is Msg.CrashTriggered -> copy(
                    crashSnapshot = msg.snapshot,
                    actions = emptyList(),
                    isRestored = false,
                    statusMessage = "崩溃已发生！点击恢复查看回溯"
                )
                is Msg.RestoredFromCrash -> copy(
                    actions = msg.snapshot.actions,
                    isRestored = true,
                    statusMessage = "已恢复 ${msg.snapshot.actions.size} 个行为"
                )
                is Msg.HistoryCleared -> copy(
                    actions = emptyList(),
                    crashSnapshot = null,
                    isRestored = false,
                    statusMessage = "历史已清空"
                )
                is Msg.ImportSuccess -> copy(
                    actions = msg.actions,
                    isRestored = false,
                    importError = null,
                    statusMessage = "已导入 ${msg.actions.size} 个行为"
                )
                is Msg.ImportFailed -> copy(
                    importError = msg.error
                )
                is Msg.ClearImportError -> copy(
                    importError = null
                )
            }
    }
}
