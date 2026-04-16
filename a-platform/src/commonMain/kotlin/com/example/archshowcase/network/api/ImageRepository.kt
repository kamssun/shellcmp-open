package com.example.archshowcase.network.api

import com.example.archshowcase.network.dto.ImagePageResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImageRepository : KoinComponent {

    private val api: ImageApi by inject()

    suspend fun getImages(offset: Int, limit: Int): Result<ImagePageResponse> =
        api.getImages(offset, limit)
}
