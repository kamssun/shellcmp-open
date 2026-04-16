package com.example.archshowcase.network.api

import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.network.dto.ImagePageResponse
import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.delay

interface ImageApi {
    suspend fun getImages(offset: Int, limit: Int): Result<ImagePageResponse>
}

class DefaultImageApi : ImageApi {
    companion object {
        private const val TAG = "ImageApi"
        private const val TOTAL_IMAGES = 5000
        private const val SIMULATED_DELAY_MS = 100L
    }

    override suspend fun getImages(offset: Int, limit: Int): Result<ImagePageResponse> = runCatching {
        Log.d(TAG) { "Fetching images: offset=$offset, limit=$limit" }

        delay(SIMULATED_DELAY_MS)

        val items = (offset until minOf(offset + limit, TOTAL_IMAGES)).map { index ->
            ImageItem(
                id = "img_$index",
                url = "https://picsum.photos/seed/image$index/400/300",
                title = "Image #${index + 1}"
            )
        }

        ImagePageResponse(
            items = items,
            total = TOTAL_IMAGES,
            hasMore = offset + limit < TOTAL_IMAGES
        )
    }.onFailure { e ->
        Log.e(TAG) { "Failed to fetch images: ${e.message}" }
    }
}
