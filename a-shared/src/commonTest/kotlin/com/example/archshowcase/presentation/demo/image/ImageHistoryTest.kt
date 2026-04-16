package com.example.archshowcase.presentation.demo.image

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.trace.scroll.ScrollPosition
import com.example.archshowcase.network.dto.ImageItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class ImageHistoryTest {

    private val initialState = ImageDemoStore.State()

    @BeforeTest
    fun setup() {
        AppConfig.enableRestore = true
    }

    @AfterTest
    fun teardown() {
        AppConfig.enableRestore = false
    }

    private val sampleImages = listOf(
        ImageItem("1", "url1", "Image 1"),
        ImageItem("2", "url2", "Image 2")
    )

    @Test
    fun `Load applyToState updates images and metadata`() {
        val record = ImageHistoryRecord(
            ImageHistoryType.Load(
                loadType = "initial",
                images = sampleImages,
                totalCount = 100,
                hasMore = true
            ),
            timestamp = 100L
        )
        val newState = record.applyToState(initialState)

        assertEquals(2, newState.images.size)
        assertEquals(100, newState.totalCount)
        assertEquals(true, newState.hasMore)
        assertFalse(newState.isInitialLoading)
        assertFalse(newState.isLoadingMore)
        assertNull(newState.error)
        assertEquals(1, newState.history.size)
    }

    @Test
    fun `Scroll applyToState updates scrollPosition`() {
        val pos = ScrollPosition(10, 200)
        val record = ImageHistoryRecord(ImageHistoryType.Scroll(pos), timestamp = 200L)
        val newState = record.applyToState(initialState)

        assertEquals(10, newState.scrollPosition.firstVisibleIndex)
        assertEquals(200, newState.scrollPosition.offset)
    }

    @Test
    fun `Load initial toIntent returns LoadInitial`() {
        val record = ImageHistoryRecord(
            ImageHistoryType.Load("initial", emptyList(), 0, false),
            timestamp = 100L
        )
        assertIs<ImageDemoStore.Intent.LoadInitial>(record.toIntent())
    }

    @Test
    fun `Load more toIntent returns LoadMore`() {
        val record = ImageHistoryRecord(
            ImageHistoryType.Load("more", emptyList(), 0, false),
            timestamp = 200L
        )
        assertIs<ImageDemoStore.Intent.LoadMore>(record.toIntent())
    }

    @Test
    fun `Load unknown type toIntent returns LoadInitial`() {
        val record = ImageHistoryRecord(
            ImageHistoryType.Load("unknown", emptyList(), 0, false),
            timestamp = 300L
        )
        assertIs<ImageDemoStore.Intent.LoadInitial>(record.toIntent())
    }

    @Test
    fun `Scroll toIntent returns UpdateScrollPosition`() {
        val pos = ScrollPosition(5, 75)
        val record = ImageHistoryRecord(ImageHistoryType.Scroll(pos), timestamp = 400L)
        val intent = record.toIntent()
        assertIs<ImageDemoStore.Intent.UpdateScrollPosition>(intent)
        assertEquals(5, intent.firstVisibleIndex)
        assertEquals(75, intent.offset)
    }

    @Test
    fun `history accumulates`() {
        val r1 = ImageHistoryRecord(
            ImageHistoryType.Load("initial", sampleImages, 100, true), 100L
        )
        val r2 = ImageHistoryRecord(ImageHistoryType.Scroll(ScrollPosition(3, 50)), 200L)

        val s1 = r1.applyToState(initialState)
        val s2 = r2.applyToState(s1)

        assertEquals(2, s2.history.size)
    }
}
