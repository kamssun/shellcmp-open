package com.example.archshowcase.presentation.timetravel

import com.example.archshowcase.core.trace.bookmark.DevBookmarkHolder
import com.example.archshowcase.core.trace.bookmark.DevBookmarkStorage
import com.example.archshowcase.core.trace.export.ExportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 书签 toggle 逻辑测试
 *
 * 测试 bookmark save/clear/exists 行为，不依赖完整 Component 生命周期。
 */
class BookmarkToggleTest {

    private lateinit var storage: FakeStorage
    private lateinit var bookmarkExists: MutableStateFlow<Boolean>
    private lateinit var bookmarkMessage: MutableStateFlow<String?>

    @BeforeTest
    fun setup() {
        storage = FakeStorage()
        DevBookmarkHolder.storage = storage
        bookmarkExists = MutableStateFlow(storage.exists())
        bookmarkMessage = MutableStateFlow(null)
    }

    @AfterTest
    fun teardown() {
        DevBookmarkHolder.storage = null
    }

    /**
     * 模拟 onBookmarkToggle 逻辑（与 DefaultTimeTravelComponent 实现一致）
     */
    private fun toggleBookmark(exportResult: ExportResult) {
        val s = DevBookmarkHolder.storage ?: return
        if (s.exists()) {
            s.delete()
            bookmarkExists.value = false
            bookmarkMessage.value = MSG_CLEARED
        } else {
            when (exportResult) {
                is ExportResult.Success -> {
                    if (s.save(exportResult.data)) {
                        bookmarkExists.value = true
                        bookmarkMessage.value = MSG_SAVED
                    } else {
                        bookmarkMessage.value = MSG_SAVE_FAILED
                    }
                }
                is ExportResult.Error -> {
                    bookmarkMessage.value = MSG_SAVE_FAILED
                }
            }
        }
    }

    @Test
    fun `save bookmark when file does not exist`() {
        val exportData = byteArrayOf(1, 2, 3)

        toggleBookmark(ExportResult.Success(exportData, "tte"))

        assertTrue(bookmarkExists.value)
        assertTrue(storage.exists())
        assertEquals(MSG_SAVED, bookmarkMessage.value)
    }

    @Test
    fun `clear bookmark when file exists`() {
        storage.save(byteArrayOf(1, 2, 3))
        bookmarkExists.value = true

        toggleBookmark(ExportResult.Success(byteArrayOf(), "tte"))

        assertFalse(bookmarkExists.value)
        assertFalse(storage.exists())
        assertEquals(MSG_CLEARED, bookmarkMessage.value)
    }

    @Test
    fun `save fails when export returns error`() {
        toggleBookmark(ExportResult.Error("export error"))

        assertFalse(bookmarkExists.value)
        assertFalse(storage.exists())
        assertEquals(MSG_SAVE_FAILED, bookmarkMessage.value)
    }

    @Test
    fun `save fails when storage write fails`() {
        storage.failOnSave = true

        toggleBookmark(ExportResult.Success(byteArrayOf(1), "tte"))

        assertFalse(bookmarkExists.value)
        assertEquals(MSG_SAVE_FAILED, bookmarkMessage.value)
    }

    @Test
    fun `no-op when storage is null`() {
        DevBookmarkHolder.storage = null

        toggleBookmark(ExportResult.Success(byteArrayOf(1), "tte"))

        assertFalse(bookmarkExists.value)
        assertNull(bookmarkMessage.value)
    }

    companion object {
        private const val MSG_SAVED = "书签已保存，重启后自动恢复"
        private const val MSG_CLEARED = "书签已清除"
        private const val MSG_SAVE_FAILED = "书签保存失败"
    }
}

private class FakeStorage(
    var failOnSave: Boolean = false
) : DevBookmarkStorage {
    private var data: ByteArray? = null

    override fun save(bytes: ByteArray): Boolean {
        if (failOnSave) return false
        data = bytes
        return true
    }

    override fun load(): ByteArray? = data
    override fun delete(): Boolean { data = null; return true }
    override fun exists(): Boolean = data != null
}
