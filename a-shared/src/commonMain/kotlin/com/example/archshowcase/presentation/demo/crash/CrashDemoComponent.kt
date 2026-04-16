package com.example.archshowcase.presentation.demo.crash

import com.example.archshowcase.presentation.navigation.AppComponentContext

interface CrashDemoComponent {
    fun onBack()
}

class DefaultCrashDemoComponent(
    context: AppComponentContext
) : CrashDemoComponent, AppComponentContext by context
