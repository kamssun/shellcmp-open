package com.example.archshowcase.presentation.demo.network

import com.example.archshowcase.core.AppConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NetworkHistoryTest {

    private val initialState = NetworkDemoStore.State()

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
    }

    @AfterTest
    fun teardown() {
        AppConfig.enableRestore = false
    }

    @Test
    fun `Request applyToState updates count and result`() {
        val record = NetworkHistoryRecord(
            NetworkHistoryType.Request(count = 3, result = "User: John"),
            timestamp = 100L
        )
        val newState = record.applyToState(initialState)

        assertEquals(3, newState.requestCount)
        assertEquals("User: John", newState.result)
        assertEquals(false, newState.isLoading)
        assertNull(newState.error)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `RequestFailed applyToState sets error`() {
        val record = NetworkHistoryRecord(
            NetworkHistoryType.RequestFailed(error = "Network error"),
            timestamp = 200L
        )
        val newState = record.applyToState(initialState)

        assertEquals("Network error", newState.error)
        assertEquals(false, newState.isLoading)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `Request toIntent returns MakeRequest`() {
        val record = NetworkHistoryRecord(
            NetworkHistoryType.Request(1, "result"),
            timestamp = 100L
        )
        assertIs<NetworkDemoStore.Intent.MakeRequest>(record.toIntent())
    }

    @Test
    fun `RequestFailed toIntent returns MakeRequest`() {
        val record = NetworkHistoryRecord(
            NetworkHistoryType.RequestFailed("err"),
            timestamp = 200L
        )
        assertIs<NetworkDemoStore.Intent.MakeRequest>(record.toIntent())
    }

    @Test
    fun `history accumulates`() {
        val r1 = NetworkHistoryRecord(NetworkHistoryType.Request(1, "r1"), 100L)
        val r2 = NetworkHistoryRecord(NetworkHistoryType.Request(2, "r2"), 200L)

        val s1 = r1.applyToState(initialState)
        val s2 = r2.applyToState(s1)

        assertEquals(2, s2.history.size)
        assertEquals(2, s2.requestCount)
    }
}
