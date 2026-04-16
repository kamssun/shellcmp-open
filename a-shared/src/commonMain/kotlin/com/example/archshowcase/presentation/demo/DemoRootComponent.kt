package com.example.archshowcase.presentation.demo

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.archshowcase.core.analytics.AnalyticsCollector
import com.example.archshowcase.core.perf.PerfMonitor
import com.example.archshowcase.core.trace.restore.registerRestorableStore
import com.example.archshowcase.presentation.demo.adaptive.AdaptiveDemoComponent
import com.example.archshowcase.presentation.demo.adaptive.DefaultAdaptiveDemoComponent
import com.example.archshowcase.presentation.demo.crash.CrashDemoComponent
import com.example.archshowcase.presentation.demo.crash.DefaultCrashDemoComponent
import com.example.archshowcase.presentation.demo.detail.DefaultDetailComponent
import com.example.archshowcase.presentation.demo.detail.DetailComponent
import com.example.archshowcase.presentation.demo.home.DefaultDemoHomeComponent
import com.example.archshowcase.presentation.demo.home.DemoHomeComponent
import com.example.archshowcase.presentation.demo.image.DefaultImageDemoComponent
import com.example.archshowcase.presentation.demo.image.ImageDemoComponent
import com.example.archshowcase.presentation.demo.network.DefaultNetworkDemoComponent
import com.example.archshowcase.presentation.demo.network.NetworkDemoComponent
import com.example.archshowcase.presentation.demo.obo.DefaultOBODemoComponent
import com.example.archshowcase.presentation.demo.obo.OBODemoComponent
import com.example.archshowcase.presentation.navigation.DEMO_NAVIGATION_STORE_NAME
import com.example.archshowcase.presentation.navigation.DefaultAppComponentContext
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.NavigationStackManager
import com.example.archshowcase.presentation.navigation.NavigationStoreFactory
import com.example.archshowcase.presentation.navigation.Route
import com.example.archshowcase.presentation.payment.DefaultPaymentComponent
import com.example.archshowcase.presentation.payment.PaymentComponent
import com.example.archshowcase.presentation.settings.DefaultSettingsComponent
import com.example.archshowcase.presentation.settings.SettingsComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface DemoRootComponent {
    val childStack: Value<ChildStack<*, DemoChild>>
    fun onBackClicked()

    sealed class DemoChild {
        data class Home(val component: DemoHomeComponent) : DemoChild()
        data class NetworkDemo(val component: NetworkDemoComponent) : DemoChild()
        data class ImageDemo(val component: ImageDemoComponent) : DemoChild()
        data class Detail(val component: DetailComponent) : DemoChild()
        data class AdaptiveDemo(val component: AdaptiveDemoComponent) : DemoChild()
        data class CrashDemo(val component: CrashDemoComponent) : DemoChild()
        data class OBODemo(val component: OBODemoComponent) : DemoChild()
        data class Settings(val component: SettingsComponent) : DemoChild()
        data class Payment(val component: PaymentComponent) : DemoChild()
    }
}

@OptIn(DelicateDecomposeApi::class)
class DefaultDemoRootComponent(
    context: AppComponentContext,
) : DemoRootComponent, ComponentContext by context, KoinComponent {

    private val navigationStoreFactory: NavigationStoreFactory by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val demoNavigationStore = registerRestorableStore(
        name = DEMO_NAVIGATION_STORE_NAME,
        factory = { navigationStoreFactory.create(DEMO_NAVIGATION_STORE_NAME) }
    )

    private val navManager = NavigationStackManager(
        store = demoNavigationStore,
        onRestoreComplete = { context.navigator.restore() },
    )

    override val childStack: Value<ChildStack<*, DemoRootComponent.DemoChild>> =
        childStack(
            source = navManager.navigation,
            serializer = Route.serializer(),
            initialStack = {
                val storeStack = demoNavigationStore.state.stack
                if (storeStack.size > 1) storeStack else listOf(Route.Home)
            },
            handleBackButton = false,
            childFactory = ::createChild
        )

    init {
        navManager.startSync(childStack, scope, backHandler)

        lifecycle.doOnDestroy {
            scope.cancel()
        }
    }

    private fun createChild(
        route: Route,
        ctx: ComponentContext,
    ): DemoRootComponent.DemoChild {
        val appCtx = DefaultAppComponentContext(ctx, navManager)
        PerfMonitor.trackPage(ctx, route.toString())
        AnalyticsCollector.trackPage(ctx, route.toString())
        return when (route) {
            is Route.Home -> DemoRootComponent.DemoChild.Home(DefaultDemoHomeComponent(appCtx))
            is Route.NetworkDemo -> DemoRootComponent.DemoChild.NetworkDemo(DefaultNetworkDemoComponent(appCtx))
            is Route.ImageDemo -> DemoRootComponent.DemoChild.ImageDemo(DefaultImageDemoComponent(appCtx))
            is Route.Detail -> DemoRootComponent.DemoChild.Detail(DefaultDetailComponent(appCtx, route.id))
            is Route.AdaptiveDemo -> DemoRootComponent.DemoChild.AdaptiveDemo(DefaultAdaptiveDemoComponent(appCtx))
            is Route.CrashDemo -> DemoRootComponent.DemoChild.CrashDemo(DefaultCrashDemoComponent(appCtx))
            is Route.OBODemo -> DemoRootComponent.DemoChild.OBODemo(DefaultOBODemoComponent(appCtx))
            is Route.Settings -> DemoRootComponent.DemoChild.Settings(DefaultSettingsComponent(appCtx))
            is Route.Payment -> DemoRootComponent.DemoChild.Payment(DefaultPaymentComponent(appCtx))
            else -> error("Non-demo route in demo stack: $route")
        }
    }

    override fun onBackClicked() = navManager.pop()
}
