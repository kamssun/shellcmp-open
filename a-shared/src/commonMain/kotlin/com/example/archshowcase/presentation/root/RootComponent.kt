package com.example.archshowcase.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.analytics.AnalyticsCollector
import com.example.archshowcase.core.analytics.AnalyticsSetup
import com.example.archshowcase.core.perf.PerfConfig
import com.example.archshowcase.core.perf.PerfMonitor
import com.example.archshowcase.core.perf.gc.GcPressureSetup
import com.example.archshowcase.core.trace.leak.LeakAuditor
import com.example.archshowcase.core.trace.restore.RestoreRegistry
import com.example.archshowcase.core.trace.restore.registerRestorableStore
import com.example.archshowcase.auth.AuthService
import com.example.archshowcase.presentation.EventMapperRegistry
import com.example.archshowcase.presentation.ExposureParamsRegistry
import com.example.archshowcase.presentation.MemorySnapshotRegistry
import com.example.archshowcase.presentation.auth.AuthObserverComponent
import com.example.archshowcase.presentation.auth.DefaultAuthObserverComponent
import com.example.archshowcase.presentation.chat.room.ChatRoomComponent
import com.example.archshowcase.presentation.chat.room.DefaultChatRoomComponent
import com.example.archshowcase.presentation.demo.DefaultDemoRootComponent
import com.example.archshowcase.presentation.demo.DemoRootComponent
import com.example.archshowcase.presentation.live.DefaultLiveComponent
import com.example.archshowcase.presentation.live.LiveComponent
import com.example.archshowcase.presentation.login.DefaultEmailLoginComponent
import com.example.archshowcase.presentation.login.DefaultLoginComponent
import com.example.archshowcase.presentation.login.EmailLoginComponent
import com.example.archshowcase.presentation.login.LoginComponent
import com.example.archshowcase.presentation.main.DefaultMainComponent
import com.example.archshowcase.presentation.main.MainComponent
import com.example.archshowcase.presentation.navigation.DefaultAppComponentContext
import com.example.archshowcase.presentation.navigation.NAVIGATION_STORE_NAME
import com.example.archshowcase.presentation.navigation.NavigationStackManager
import com.example.archshowcase.presentation.navigation.NavigationStore
import com.example.archshowcase.presentation.navigation.NavigationStoreFactory
import com.example.archshowcase.presentation.navigation.Route
import com.example.archshowcase.presentation.navigation.loadNavigationModule
import com.example.archshowcase.presentation.payment.DefaultPaymentComponent
import com.example.archshowcase.presentation.payment.PaymentComponent
import com.example.archshowcase.presentation.settings.DefaultSettingsComponent
import com.example.archshowcase.presentation.settings.SettingsComponent
import com.example.archshowcase.presentation.timetravel.DefaultTimeTravelComponent
import com.example.archshowcase.presentation.timetravel.TimeTravelComponent
import com.example.archshowcase.presentation.timetravel.TimeTravelComponentHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>
    val timeTravelComponent: TimeTravelComponent?

    fun onBackClicked()

    sealed class Child {
        data class Login(val component: LoginComponent) : Child()
        data class EmailLogin(val component: EmailLoginComponent) : Child()
        data class Demo(val component: DemoRootComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
        data class Live(val component: LiveComponent) : Child()
        data class Payment(val component: PaymentComponent) : Child()
        data class Main(val component: MainComponent) : Child()
        data class ChatRoom(val component: ChatRoomComponent) : Child()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    init {
        loadNavigationModule()
    }

    private val navigationStoreFactory: NavigationStoreFactory by inject()
    private val authService: AuthService by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initialRoute: Route = when {
        !AppConfig.skipLogin && !authService.isLoggedIn() -> Route.Login
        AppConfig.useDemoMode -> Route.Home
        else -> Route.Main
    }

    private val navigationStore = registerRestorableStore(
        name = NAVIGATION_STORE_NAME,
        factory = { navigationStoreFactory.create() }
    )

    private val navManager = NavigationStackManager(store = navigationStore)

    @OptIn(DelicateDecomposeApi::class)
    override val timeTravelComponent: TimeTravelComponent? =
        if (AppConfig.enableRestore) DefaultTimeTravelComponent(
            DefaultAppComponentContext(childContext(key = "timeTravel"), navManager)
        )
        else null

    override val childStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navManager.navigation,
            serializer = Route.serializer(),
            initialConfiguration = initialRoute,
            handleBackButton = false,
            childFactory = ::createChild
        )

    /** 监听 authState 驱动登录/登出路由切换，必须在 startSync 之后创建 */
    @OptIn(DelicateDecomposeApi::class)
    private val authObserver: AuthObserverComponent

    init {
        timeTravelComponent?.let { TimeTravelComponentHolder.set(it) }

        if (initialRoute != Route.Home && navigationStore.state.stack == listOf(Route.Home)) {
            navigationStore.accept(NavigationStore.Intent.ReplaceAll(listOf(initialRoute)))
        }

        navManager.startSync(childStack, scope, backHandler)

        authObserver = DefaultAuthObserverComponent(
            DefaultAppComponentContext(childContext(key = "auth"), navManager)
        )

        if (!AppRuntimeState.isInPreview) {
            LeakAuditor.start(scope)
            PerfMonitor.start(PerfConfig(enabled = AppConfig.enablePerfMonitor))
            PerfMonitor.pageTracker?.start(initialRoute.toString())
            GcPressureSetup.storeSnapshotProvider = ::collectStoreMemorySnapshots
            // Analytics 独立初始化
            if (AppConfig.enableAnalytics) {
                AnalyticsSetup.start(
                    scope,
                    eventMapper = EventMapperRegistry::map,
                    paramsExtractor = ExposureParamsRegistry::extract,
                )
            }
        }

        lifecycle.doOnDestroy {
            AnalyticsSetup.stop()
            PerfMonitor.stop()
            TimeTravelComponentHolder.clear()
            LeakAuditor.stop()
            scope.cancel()
        }
    }

    private fun createChild(route: Route, ctx: ComponentContext): RootComponent.Child {
        val appCtx = DefaultAppComponentContext(ctx, navManager)
        PerfMonitor.trackPage(ctx, route.toString())
        // Route.Home 是容器（DemoRootComponent 有自己的子导航会 trackPage），跳过避免重复
        if (route !is Route.Home) {
            AnalyticsCollector.trackPage(ctx, route.toString())
        }
        return when (route) {
            is Route.Login -> RootComponent.Child.Login(DefaultLoginComponent(appCtx))
            is Route.EmailLogin -> RootComponent.Child.EmailLogin(DefaultEmailLoginComponent(appCtx))
            is Route.Home -> RootComponent.Child.Demo(DefaultDemoRootComponent(appCtx))
            is Route.Settings -> RootComponent.Child.Settings(DefaultSettingsComponent(appCtx))
            is Route.Live -> RootComponent.Child.Live(DefaultLiveComponent(appCtx))
            is Route.Payment -> RootComponent.Child.Payment(DefaultPaymentComponent(appCtx))
            is Route.Main -> RootComponent.Child.Main(DefaultMainComponent(appCtx))
            is Route.ChatRoom -> RootComponent.Child.ChatRoom(
                DefaultChatRoomComponent(appCtx, route.conversationId)
            )
            else -> error("Demo route in root stack: $route")
        }
    }

    override fun onBackClicked() = navManager.pop()
}

/**
 * 遍历 RestoreRegistry 中活跃的 Store，通过 KSP 生成的 MemorySnapshotRegistry
 * 收集集合字段 size。
 */
private fun collectStoreMemorySnapshots(): Map<String, Map<String, Int>> {
    val result = mutableMapOf<String, Map<String, Int>>()
    for (name in RestoreRegistry.getAuditInfo().names) {
        val store = RestoreRegistry.findStore(name) ?: continue
        MemorySnapshotRegistry.snapshot(store.state)?.let { result[name] = it }
    }
    return result
}
