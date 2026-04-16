package com.example.archshowcase.presentation.demo

import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.presentation.demo.image.ImageDemoStore
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore
import com.example.archshowcase.presentation.navigation.NavigationStore
import com.example.archshowcase.presentation.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StoreStateTest {

    // --- NavigationStore.State computed properties ---

    @Test
    fun `NavigationStore State currentRoute returns last in stack`() {
        val state = NavigationStore.State(stack = listOf(Route.Home, Route.Settings))
        assertEquals(Route.Settings, state.currentRoute)
    }

    // --- ImageDemoStore.State computed properties ---

    @Test
    fun `ImageDemoStore State isEmpty false when loading`() {
        val state = ImageDemoStore.State(isInitialLoading = true)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `ImageDemoStore State isEmpty false when has images`() {
        val state = ImageDemoStore.State(images = listOf(ImageItem("1", "url", "title")))
        assertFalse(state.isEmpty)
    }

    @Test
    fun `ImageDemoStore State canLoadMore false when no more`() {
        assertFalse(ImageDemoStore.State(hasMore = false).canLoadMore)
    }

    @Test
    fun `ImageDemoStore State canLoadMore false when loading more`() {
        assertFalse(ImageDemoStore.State(isLoadingMore = true).canLoadMore)
    }

    @Test
    fun `ImageDemoStore State canLoadMore false when initial loading`() {
        assertFalse(ImageDemoStore.State(isInitialLoading = true).canLoadMore)
    }

    // --- toTraceString formatting ---

    @Test
    fun `NetworkDemoStore Msg RequestSuccess toTraceString`() {
        val msg = NetworkDemoStore.Msg.RequestSuccess(count = 5, result = "ok", timestamp = 100L)
        assertEquals("RequestSuccess(count=5)", msg.toTraceString())
    }

    @Test
    fun `ImageDemoStore Msg ImagesLoaded toTraceString`() {
        val msg = ImageDemoStore.Msg.ImagesLoaded(
            images = listOf(ImageItem("1", "u", "t"), ImageItem("2", "u", "t")),
            totalCount = 100,
            hasMore = true,
            loadType = "initial",
            timestamp = 100L
        )
        assertEquals("ImagesLoaded(count=2, total=100)", msg.toTraceString())
    }
}
