package com.example.archshowcase.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCircularProgress
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTextField
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.btn_back
import com.example.archshowcase.resources.btn_dismiss
import com.example.archshowcase.resources.btn_resend_code
import com.example.archshowcase.resources.btn_send_code
import com.example.archshowcase.resources.btn_verify_login
import com.example.archshowcase.resources.label_email
import com.example.archshowcase.resources.label_verification_code
import com.example.archshowcase.resources.title_email_login

@Composable
fun EmailLoginContent(component: EmailLoginComponent) {
    val state = component.state.rememberFields()
    var email by rememberSaveable { mutableStateOf(component.state.value.currentEmail) }
    var code by rememberSaveable { mutableStateOf("") }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_email_login), style = AppTheme.typography.titleMedium) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            AppTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                label = tr(Res.string.label_email),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppButton(
                onClick = { component.onSendCode(email.trim()) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppText(if (state.emailCodeSent) tr(Res.string.btn_resend_code) else tr(Res.string.btn_send_code))
            }

            if (state.emailCodeSent) {
                Spacer(modifier = Modifier.height(20.dp))

                AppTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    label = tr(Res.string.label_verification_code),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppButton(
                    onClick = { component.onVerifyCode(email.trim(), code.trim()) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppText(tr(Res.string.btn_verify_login))
                }
            }

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                AppCircularProgress()
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
}

@Preview
@Composable
fun EmailLoginContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultEmailLoginComponent(componentContext) }
    EmailLoginContent(component)
}
