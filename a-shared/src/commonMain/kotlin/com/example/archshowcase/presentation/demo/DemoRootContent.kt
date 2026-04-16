package com.example.archshowcase.presentation.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.example.archshowcase.presentation.demo.adaptive.AdaptiveDemoContent
import com.example.archshowcase.presentation.demo.crash.CrashDemoContent
import com.example.archshowcase.presentation.demo.detail.DetailContent
import com.example.archshowcase.presentation.demo.home.DemoHomeContent
import com.example.archshowcase.presentation.demo.image.ImageDemoContent
import com.example.archshowcase.presentation.demo.network.NetworkDemoContent
import com.example.archshowcase.presentation.demo.obo.OBODemoContent
import com.example.archshowcase.presentation.payment.PaymentContent
import com.example.archshowcase.presentation.settings.SettingsContent

@Composable
fun DemoRootContent(
    component: DemoRootComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.childStack,
        modifier = modifier.fillMaxSize(),
        animation = stackAnimation(fade()),
    ) { child ->
        when (val instance = child.instance) {
            is DemoRootComponent.DemoChild.Home -> DemoHomeContent(component = instance.component)
            is DemoRootComponent.DemoChild.NetworkDemo -> NetworkDemoContent(component = instance.component)
            is DemoRootComponent.DemoChild.ImageDemo -> ImageDemoContent(component = instance.component)
            is DemoRootComponent.DemoChild.Detail -> DetailContent(component = instance.component)
            is DemoRootComponent.DemoChild.AdaptiveDemo -> AdaptiveDemoContent(component = instance.component)
            is DemoRootComponent.DemoChild.CrashDemo -> CrashDemoContent(component = instance.component)
            is DemoRootComponent.DemoChild.OBODemo -> OBODemoContent(component = instance.component)
            is DemoRootComponent.DemoChild.Settings -> SettingsContent(component = instance.component)
            is DemoRootComponent.DemoChild.Payment -> PaymentContent(component = instance.component)
        }
    }
}
