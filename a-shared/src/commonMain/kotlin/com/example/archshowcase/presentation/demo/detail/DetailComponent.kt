package com.example.archshowcase.presentation.demo.detail

import com.example.archshowcase.presentation.navigation.AppComponentContext

interface DetailComponent {
    val itemId: String

    fun onBack()
}

class DefaultDetailComponent(
    context: AppComponentContext,
    override val itemId: String
) : DetailComponent, AppComponentContext by context
