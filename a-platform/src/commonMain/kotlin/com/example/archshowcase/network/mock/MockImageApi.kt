package com.example.archshowcase.network.mock

import com.example.archshowcase.network.api.ImageApi
import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.network.dto.ImagePageResponse
import com.example.archshowcase.core.util.Log

class MockImageApi : ImageApi {
    companion object {
        private const val TAG = "MockImageApi"
        private const val TOTAL_IMAGES = 100
    }

    override suspend fun getImages(offset: Int, limit: Int): Result<ImagePageResponse> = runCatching {
        Log.d(TAG) { "Mock fetching images: offset=$offset, limit=$limit" }

        val items = (offset until minOf(offset + limit, TOTAL_IMAGES)).map { index ->
            ImageItem(
                id = "mock_$index",
                url = "https://picsum.photos/seed/mock$index/400/300",
                title = "Mock Image #${index + 1}"
            )
        }

        ImagePageResponse(
            items = items,
            total = TOTAL_IMAGES,
            hasMore = offset + limit < TOTAL_IMAGES
        )
    }
}
