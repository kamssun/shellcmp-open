package com.example.archshowcase.core.trace.export

import com.example.archshowcase.core.trace.restore.RestorableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private data class ContextTestState(val value: String = "") : RestorableState {
    override fun hasValidData() = value.isNotEmpty()
}

class ExportContextTest {

    @Test
    fun `getState returns typed state for matching key`() {
        val state = ContextTestState("hello")
        val context = ExportContext(restorableStates = mapOf("store1" to state))

        val result = context.getState<ContextTestState>("store1")
        assertEquals(ContextTestState("hello"), result)
    }

    @Test
    fun `getState returns null for missing key`() {
        val context = ExportContext(restorableStates = emptyMap())

        assertNull(context.getState<ContextTestState>("missing"))
    }

    @Test
    fun `getExtra returns typed extra for matching key`() {
        val context = ExportContext(
            restorableStates = emptyMap(),
            extras = mapOf("count" to 42)
        )

        assertEquals(42, context.getExtra<Int>("count"))
    }

    @Test
    fun `getExtra returns null for missing key`() {
        val context = ExportContext(restorableStates = emptyMap())

        assertNull(context.getExtra<String>("missing"))
    }

    @Test
    fun `getExtra returns null for wrong type`() {
        val context = ExportContext(
            restorableStates = emptyMap(),
            extras = mapOf("count" to 42)
        )

        assertNull(context.getExtra<String>("count"))
    }
}
