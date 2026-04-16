package com.example.archshowcase.presentation.demo.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.navigation.Route
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.btn_adaptive_demo
import com.example.archshowcase.resources.btn_crash_demo
import com.example.archshowcase.resources.btn_image_demo
import com.example.archshowcase.resources.btn_network_demo
import com.example.archshowcase.resources.btn_obo_demo
import com.example.archshowcase.resources.btn_payment
import com.example.archshowcase.resources.btn_settings
import com.example.archshowcase.resources.text_demo_home_subtitle
import com.example.archshowcase.resources.title_demo_home

@Composable
fun DemoHomeContent(component: DemoHomeComponent) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppText(
            tr(Res.string.title_demo_home),
            style = AppTheme.typography.headlineLarge
        )
        AppText(
            tr(Res.string.text_demo_home_subtitle),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        AppButton(
            onClick = { component.onNavigate(Route.NetworkDemo) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_network_demo))
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            onClick = { component.onNavigate(Route.ImageDemo) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_image_demo))
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            onClick = { component.onNavigate(Route.AdaptiveDemo) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_adaptive_demo))
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            onClick = { component.onNavigate(Route.CrashDemo) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_crash_demo))
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            onClick = { component.onNavigate(Route.OBODemo) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_obo_demo))
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            onClick = { component.onNavigate(Route.Settings) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_settings))
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            onClick = { component.onNavigate(Route.Payment) },
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_payment))
        }
    }
}

@Preview
@Composable
fun DemoHomeContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultDemoHomeComponent(componentContext) }
    DemoHomeContent(component)
}
