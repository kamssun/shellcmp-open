package com.example.archshowcase.presentation.demo.home

import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.Route

interface DemoHomeComponent {
    fun onNavigate(route: Route)
}

class DefaultDemoHomeComponent(
    context: AppComponentContext
) : DemoHomeComponent, AppComponentContext by context {

    override fun onNavigate(route: Route) = navigator.push(route)
}
