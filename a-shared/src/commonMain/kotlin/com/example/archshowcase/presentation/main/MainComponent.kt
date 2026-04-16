package com.example.archshowcase.presentation.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import com.example.archshowcase.core.trace.restore.registerRestorableStore
import com.example.archshowcase.presentation.chat.list.ConversationListComponent
import com.example.archshowcase.presentation.chat.list.DefaultConversationListComponent
import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.presentation.navigation.DefaultAppComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MainComponent {
    val state: StateFlow<MainTabStore.State>
    val tabs: List<TabChild>
    fun onTabSelected(index: Int)

    sealed class TabChild {
        data class Home(val component: TmpTabComponent) : TabChild()
        data class Discover(val component: TmpTabComponent) : TabChild()
        data class Chat(val component: ConversationListComponent) : TabChild()
        data class Me(val component: TmpTabComponent) : TabChild()
    }
}

class TmpTabComponent(
    componentContext: ComponentContext,
    val tab: MainTab,
) : ComponentContext by componentContext

@OptIn(ExperimentalCoroutinesApi::class, DelicateDecomposeApi::class)
class DefaultMainComponent(
    context: AppComponentContext,
) : MainComponent, AppComponentContext by context, KoinComponent {

    init { loadMainTabModule() }

    private val storeFactory: MainTabStoreFactory by inject()

    private val store = registerRestorableStore(
        name = MAIN_TAB_STORE_NAME,
        factory = { storeFactory.create() }
    )

    override val state: StateFlow<MainTabStore.State> = store.stateFlow

    override val tabs: List<MainComponent.TabChild> = MainTab.entries.map { tab ->
        val ctx = childContext(key = "tab_${tab.name}")
        when (tab) {
            MainTab.Home -> MainComponent.TabChild.Home(TmpTabComponent(ctx, tab))
            MainTab.Discover -> MainComponent.TabChild.Discover(TmpTabComponent(ctx, tab))
            MainTab.Chat -> {
                val appCtx: AppComponentContext = DefaultAppComponentContext(ctx, navigator)
                MainComponent.TabChild.Chat(
                    DefaultConversationListComponent(context = appCtx)
                )
            }
            MainTab.Me -> MainComponent.TabChild.Me(TmpTabComponent(ctx, tab))
        }
    }

    override fun onTabSelected(index: Int) {
        if (index != store.state.selectedIndex) {
            store.accept(MainTabStore.Intent.SelectTab(index))
        }
    }
}
