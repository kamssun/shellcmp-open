package com.example.archshowcase.presentation.main

import androidx.compose.foundation.background
import com.example.archshowcase.core.analytics.appClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.chat.list.ConversationListContent
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme

@Composable
fun MainContent(component: MainComponent) {
    val state by component.state.collectAsState()
    val selectedChild = component.tabs.getOrNull(state.selectedIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedChild) {
                is MainComponent.TabChild.Chat ->
                    ConversationListContent(component = selectedChild.component)
                is MainComponent.TabChild.Home ->
                    TmpContent(tabName = tr(selectedChild.component.tab.titleRes))
                is MainComponent.TabChild.Discover ->
                    TmpContent(tabName = tr(selectedChild.component.tab.titleRes))
                is MainComponent.TabChild.Me ->
                    TmpContent(tabName = tr(selectedChild.component.tab.titleRes))
                null -> {}
            }
        }

        AppTabBar(
            selectedIndex = state.selectedIndex,
            onTabSelected = component::onTabSelected,
        )
    }
}

@Composable
private fun TmpContent(tabName: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = tabName,
            style = AppTheme.typography.headlineMedium,
            color = AppTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun AppTabBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppTheme.colors.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val color = if (selected) AppTheme.colors.primary else AppTheme.colors.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1f)
                    .appClickable(component = "AppTabBar", onClick = { onTabSelected(index) })
                    .height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = tr(tab.titleRes),
                    style = AppTheme.typography.labelMedium,
                    color = color,
                )
            }
        }
    }
}
