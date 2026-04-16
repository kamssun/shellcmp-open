package com.example.archshowcase.core.trace.restore

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.AppRuntimeState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private data class ResolverTestState(val data: String = "") : RestorableState {
    override fun hasValidData() = data.isNotEmpty()
}

class InitialStateResolverTest {

    @BeforeTest
    fun setup() {
        RestoreRegistry.clear()
        AppRuntimeState.verificationMode = false
    }

    @AfterTest
    fun teardown() {
        RestoreRegistry.clear()
        AppConfig.enableRestore = true
        AppRuntimeState.verificationMode = false
    }

    @Test
    fun `resolveInitialState delegates to InitialStateResolver`() {
        AppConfig.enableRestore = false
        val result = RestoreRegistry.resolveInitialState<ResolverTestState>("store")
        assertNull(result)
    }

    // ─── verificationMode 测试 ──────────────────────────────────

    @Test
    fun `verificationMode returns prefilled state from RestoreRegistry`() {
        AppConfig.enableRestore = true
        AppRuntimeState.verificationMode = true

        val prefilled = ResolverTestState(data = "prefilled_from_tte")
        RestoreRegistry.updateSnapshotOrCreate("TestStore", prefilled)

        val result = InitialStateResolver.resolve<ResolverTestState>("TestStore")
        assertEquals(prefilled, result)
    }

    @Test
    fun `verificationMode returns null when no snapshot exists`() {
        AppConfig.enableRestore = true
        AppRuntimeState.verificationMode = true

        val result = InitialStateResolver.resolve<ResolverTestState>("NonExistentStore")
        assertNull(result)
    }

    @Test
    fun `verificationMode returns null for invalid snapshot`() {
        AppConfig.enableRestore = true
        AppRuntimeState.verificationMode = true

        val invalid = ResolverTestState(data = "")  // hasValidData() == false
        RestoreRegistry.updateSnapshotOrCreate("TestStore", invalid)

        val result = InitialStateResolver.resolve<ResolverTestState>("TestStore")
        assertNull(result)
    }

    @Test
    fun `IDLE mode returns null when verificationMode is false`() {
        AppConfig.enableRestore = true
        AppRuntimeState.verificationMode = false

        val state = ResolverTestState(data = "should_not_return")
        RestoreRegistry.updateSnapshotOrCreate("TestStore", state)

        val result = InitialStateResolver.resolve<ResolverTestState>("TestStore")
        assertNull(result)
    }

    @Test
    fun `verificationMode returns null when enableRestore is false`() {
        AppConfig.enableRestore = false
        AppRuntimeState.verificationMode = true

        val state = ResolverTestState(data = "prefilled")
        RestoreRegistry.updateSnapshotOrCreate("TestStore", state)

        val result = InitialStateResolver.resolve<ResolverTestState>("TestStore")
        assertNull(result)
    }
}
