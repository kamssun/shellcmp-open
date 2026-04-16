package com.example.archshowcase.im.service

import com.example.archshowcase.im.model.ImConfig
import com.example.archshowcase.im.model.ImStatusCode
import kotlinx.coroutines.flow.StateFlow

interface ImService {
    val statusFlow: StateFlow<ImStatusCode>

    fun initialize(config: ImConfig)
    fun login(onSuccess: () -> Unit = {}, onError: (code: Int, msg: String) -> Unit = { _, _ -> })
    fun logout()
    fun isLoggedIn(): Boolean
    fun destroy()
}
