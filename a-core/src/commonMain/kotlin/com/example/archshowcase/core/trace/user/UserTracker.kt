package com.example.archshowcase.core.trace.user

import com.example.archshowcase.core.isDebug
import com.example.archshowcase.core.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object IntentTracker {
    private const val MAX_SIZE = 50

    private val intents = ArrayDeque<IntentRecord>(MAX_SIZE + 1)

    fun record(storeName: String, intent: Any) {
        val traceString = when {
            intent is UserTraceable && isDebug() -> intent.toDebugString()
            intent is UserTraceable -> intent.toTraceString()
            else -> intent.toString()
        }

        intents.addLast(
            IntentRecord(
                store = storeName,
                intent = traceString,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
        if (intents.size > MAX_SIZE) intents.removeFirst()
    }

    fun dump() {
        val current = snapshot()
        Log.e { "=== Recent Intents (${current.size}) ===" }
        current.forEachIndexed { i, record ->
            Log.e { "${i + 1}. [${record.store}] ${record.intent}" }
        }
    }

    fun snapshot(): List<IntentRecord> = intents.toList()

    fun clear() {
        intents.clear()
    }

    fun exportJson(): String {
        val export = IntentTraceExport(
            exportTime = Clock.System.now().toEpochMilliseconds(),
            intents = snapshot()
        )
        return Json.encodeToString(export)
    }
}

@Serializable
data class IntentRecord(
    val store: String,
    val intent: String,
    val timestamp: Long
)

@Serializable
data class IntentTraceExport(
    val version: Int = 1,
    val exportTime: Long,
    val intents: List<IntentRecord>
)
