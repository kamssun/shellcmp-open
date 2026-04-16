package com.example.archshowcase.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppSwitch
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.theme.AppTheme

@Composable
fun SettingsContent(component: SettingsComponent) {
    val state = component.state.rememberFields()

    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_settings), style = AppTheme.typography.titleMedium) },
                navigationIcon = {
                    AppTextButton(onClick = { component.onBackClicked() }) {
                        AppText(tr(Res.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            tr(Res.string.text_obo_scheduler),
                            style = AppTheme.typography.titleMedium
                        )
                        AppText(
                            tr(Res.string.text_obo_scheduler_desc),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    AppSwitch(
                        checked = state.useOBOScheduler,
                        onCheckedChange = { component.onOBOSchedulerToggle(it) }
                    )
                }
            }
        }
    }
}
