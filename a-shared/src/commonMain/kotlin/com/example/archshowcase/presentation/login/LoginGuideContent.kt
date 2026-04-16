package com.example.archshowcase.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCircularProgress
import com.example.archshowcase.presentation.component.AppOutlinedButton
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme

@Composable
fun LoginGuideContent(component: LoginComponent) {
    val state = component.state.rememberFields()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(
            text = tr(Res.string.title_login),
            style = AppTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            onClick = { component.onGoogleLogin() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_google_login))
        }

        if (component.showAppleLogin) {
            Spacer(modifier = Modifier.height(12.dp))
            AppOutlinedButton(
                onClick = { component.onAppleLogin() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppText(tr(Res.string.btn_apple_login))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AppOutlinedButton(
            onClick = { component.onEmailLogin() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(tr(Res.string.btn_email_code_login))
        }

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(20.dp))
            AppCircularProgress(modifier = Modifier.size(24.dp))
        }

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            AppText(
                text = message,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.error
            )
            AppTextButton(onClick = { component.onDismissError() }) {
                AppText(tr(Res.string.btn_dismiss))
            }
        }
    }
}

@Preview
@Composable
fun LoginGuideContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultLoginComponent(componentContext) }
    LoginGuideContent(component)
}
