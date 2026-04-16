package com.example.archshowcase.core.trace.tester

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.tester.TestTraceStore.Intent
import com.example.archshowcase.core.trace.tester.TestTraceStore.Label
import com.example.archshowcase.core.trace.tester.TestTraceStore.State
import kotlinx.serialization.Serializable

interface TestTraceStore : Store<Intent, State, Label> {

    sealed interface Intent : JvmSerializable {
        data class RecordAction(val action: UserAction) : Intent
        data object TriggerCrash : Intent
        data object RestoreFromCrash : Intent
        data object ClearHistory : Intent
        data class ImportActions(val json: String) : Intent
        data class ImportActionsList(val actions: List<UserAction>) : Intent
        data class SetImportError(val error: String) : Intent
        data object ClearImportError : Intent
    }

    @Serializable
    data class UserAction(
        val id: Int,
        val name: String,
        val timestamp: Long
    ) : JvmSerializable

    @Serializable
    data class CrashSnapshot(
        val actions: List<UserAction>,
        val crashTime: Long,
        val errorMessage: String
    ) : JvmSerializable

    @Serializable
    data class ActionTraceExport(
        val version: Int = 1,
        val exportTime: Long,
        val actions: List<UserAction>
    ) : JvmSerializable

    data class State(
        val actions: List<UserAction> = emptyList(),
        val crashSnapshot: CrashSnapshot? = null,
        val isRestored: Boolean = false,
        val statusMessage: String = "记录用户行为，模拟崩溃后回溯",
        val importError: String? = null
    ) : JvmSerializable

    sealed interface Label : JvmSerializable {
        data class CrashOccurred(val snapshot: CrashSnapshot) : Label
        data object Restored : Label
    }
}
