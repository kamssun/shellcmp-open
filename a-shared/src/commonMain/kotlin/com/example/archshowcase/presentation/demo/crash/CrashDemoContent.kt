package com.example.archshowcase.presentation.demo.crash

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
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppOutlinedButton
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.core.util.Log

private const val TAG = "CrashDemo"

@Composable
fun CrashDemoContent(component: CrashDemoComponent) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_crash_demo), style = AppTheme.typography.titleMedium) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppText(tr(Res.string.text_verify_crashkios), style = AppTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppText(tr(Res.string.text_feature_desc), style = AppTheme.typography.labelMedium)
                    AppText(
                        tr(Res.string.text_crash_features),
                        style = AppTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppButton(
                onClick = {
                    Log.d(TAG) { "This is a debug log" }
                    Log.i(TAG) { "This is an info log" }
                    Log.w(TAG) { "This is a warning log" }
                }
            ) {
                AppText(tr(Res.string.btn_log_kermit))
            }

            Spacer(modifier = Modifier.height(8.dp))

            AppOutlinedButton(
                onClick = {
                    try {
                        throw RuntimeException("Test exception from Demo")
                    } catch (e: Exception) {
                        Log.e("CrashDemo", e) { "Caught test exception" }
                    }
                }
            ) {
                AppText(tr(Res.string.btn_simulate_exception))
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                onClick = {
                    throw RuntimeException("真实崩溃测试 - 验证 IntentTracker")
                },
                containerColor = AppTheme.colors.error,
                contentColor = AppTheme.colors.onError
            ) {
                AppText(tr(Res.string.btn_trigger_crash))
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppText(
                tr(Res.string.text_crash_hint),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun CrashDemoContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultCrashDemoComponent(componentContext) }
    CrashDemoContent(component)
}
