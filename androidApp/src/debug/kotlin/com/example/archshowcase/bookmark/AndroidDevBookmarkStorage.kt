package com.example.archshowcase.bookmark

import android.os.Environment
import com.example.archshowcase.core.trace.bookmark.DevBookmarkStorage
import com.example.archshowcase.core.util.Log
import java.io.File

/**
 * Android 平台的开发书签存储
 *
 * 固定路径：Downloads/dev_bookmark.tte
 */
class AndroidDevBookmarkStorage : DevBookmarkStorage {

    private val file: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            FILE_NAME
        )

    override fun save(bytes: ByteArray): Boolean = try {
        file.writeBytes(bytes)
        true
    } catch (e: Exception) {
        Log.e(TAG) { "Save failed: ${e.message}" }
        false
    }

    override fun load(): ByteArray? = try {
        val f = file
        if (f.exists()) f.readBytes() else null
    } catch (e: Exception) {
        Log.e(TAG) { "Load failed: ${e.message}" }
        null
    }

    override fun delete(): Boolean = try {
        file.delete()
    } catch (e: Exception) {
        Log.e(TAG) { "Delete failed: ${e.message}" }
        false
    }

    override fun exists(): Boolean = try {
        file.exists()
    } catch (e: Exception) {
        false
    }

    companion object {
        private const val TAG = "DevBookmark"
        private const val FILE_NAME = "dev_bookmark.tte"
    }
}
