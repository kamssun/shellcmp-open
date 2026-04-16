package com.example.archshowcase.presentation.demo.adaptive

import com.example.archshowcase.presentation.navigation.AppComponentContext

interface AdaptiveDemoComponent {
    fun onBack()
}

class DefaultAdaptiveDemoComponent(
    context: AppComponentContext
) : AdaptiveDemoComponent, AppComponentContext by context
