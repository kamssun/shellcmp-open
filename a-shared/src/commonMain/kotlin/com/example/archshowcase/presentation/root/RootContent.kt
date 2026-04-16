package com.example.archshowcase.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.example.archshowcase.i18n.LocalStringProvider
import com.example.archshowcase.i18n.StringProvider
import org.koin.compose.koinInject
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.demo.DemoRootContent
import com.example.archshowcase.presentation.chat.room.ChatRoomContent
import com.example.archshowcase.presentation.main.MainContent
import com.example.archshowcase.presentation.live.LiveContent
import com.example.archshowcase.presentation.login.EmailLoginContent
import com.example.archshowcase.presentation.payment.PaymentContent
import com.example.archshowcase.presentation.login.LoginGuideContent
import com.example.archshowcase.presentation.settings.SettingsContent
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.presentation.timetravel.TimeTravelFloatingPanel

@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier
) {
    AppTheme {
        CompositionLocalProvider(LocalStringProvider provides koinInject<StringProvider>()) {
            AppScaffold { paddingValues ->
                Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
                    Children(
                        stack = component.childStack,
                        modifier = Modifier.fillMaxSize(),
                        animation = stackAnimation(fade())
                    ) { child ->
                        when (val instance = child.instance) {
                            is RootComponent.Child.Login -> LoginGuideContent(component = instance.component)
                            is RootComponent.Child.EmailLogin -> EmailLoginContent(component = instance.component)
                            is RootComponent.Child.Demo -> DemoRootContent(component = instance.component)
                            is RootComponent.Child.Settings -> SettingsContent(component = instance.component)
                            is RootComponent.Child.Live -> LiveContent(component = instance.component)
                            is RootComponent.Child.Payment -> PaymentContent(component = instance.component)
                            is RootComponent.Child.Main -> MainContent(component = instance.component)
                            is RootComponent.Child.ChatRoom -> ChatRoomContent(component = instance.component)
                        }
                    }

                    component.timeTravelComponent?.let { timeTravel ->
                        TimeTravelFloatingPanel(
                            component = timeTravel,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
