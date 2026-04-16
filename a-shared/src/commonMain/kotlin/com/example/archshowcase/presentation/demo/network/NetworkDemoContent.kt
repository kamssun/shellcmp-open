package com.example.archshowcase.presentation.demo.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppCircularProgress
import com.example.archshowcase.presentation.component.AppOutlinedButton
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme

@Composable
fun NetworkDemoContent(component: NetworkDemoComponent) {
    val state = component.state.rememberFields()

    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_network_demo), style = AppTheme.typography.titleMedium) },
                navigationIcon = {
                    AppTextButton(onClick = { component.onBack() }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText(tr(Res.string.text_verify_mvikotlin), style = AppTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                onClick = { component.onMakeRequest() },
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    AppCircularProgress(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2f
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                AppText(if (state.isLoading) tr(Res.string.btn_requesting) else tr(Res.string.btn_make_request))
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    state.result,
                    modifier = Modifier.padding(16.dp),
                    style = AppTheme.typography.bodyMedium
                )
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AppText(
                    state.error!!,
                    color = AppTheme.colors.error,
                    style = AppTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppText(tr(Res.string.text_test_navigation), style = AppTheme.typography.titleSmall)

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppOutlinedButton(onClick = { component.onNavigateToDetail("item_001") }) {
                    AppText(tr(Res.string.btn_detail_001))
                }
                AppOutlinedButton(onClick = { component.onNavigateToDetail("item_002") }) {
                    AppText(tr(Res.string.btn_detail_002))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppText(tr(Res.string.text_mvikotlin_arch), style = AppTheme.typography.labelMedium)
                    AppText(
                        tr(Res.string.text_mvikotlin_features),
                        style = AppTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NetworkDemoContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultNetworkDemoComponent(componentContext) }
    NetworkDemoContent(component)
}
