package com.example.archshowcase.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.di.createPreviewPlatformModule
import com.example.archshowcase.di.getAppModules
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.DefaultAppComponentContext
import com.example.archshowcase.presentation.navigation.Navigator
import com.example.archshowcase.presentation.navigation.Route
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.ColorImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.example.archshowcase.i18n.LocalStringProvider
import com.example.archshowcase.i18n.StringProvider
import com.example.archshowcase.presentation.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

/** Preview 用的空 Navigator */
private object PreviewNavigator : Navigator {
    override val currentRoute: Route = Route.Home
    override fun push(route: Route) = Unit
    override fun pop() = Unit
    override fun replaceAll(vararg routes: Route) = Unit
    override fun restore() = Unit
}

/**
 * 预览包装器，提供完整的 Koin DI 环境和 AppComponentContext
 */
@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@Composable
fun PreviewWrapper(
    content: @Composable (AppComponentContext) -> Unit
) {
    remember {
        AppRuntimeState.isInPreview = true
    }
    val platformModule = createPreviewPlatformModule()
    val appContext = remember {
        DefaultAppComponentContext(
            DefaultComponentContext(lifecycle = LifecycleRegistry()),
            PreviewNavigator,
        )
    }

    KoinApplication(application = {
        modules(getAppModules(platformModule))
    }) {
        CompositionLocalProvider(
            LocalStringProvider provides koinInject<StringProvider>(),
            LocalInspectionMode provides true,
            LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler {
                ColorImage(0xFFE0E0E0.toInt())
            },
        ) {
            AppTheme {
                Box(modifier = Modifier.background(AppTheme.colors.background)) {
                    content(appContext)
                }
            }
        }
    }
}
