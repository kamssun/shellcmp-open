package com.example.archshowcase.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.presentation.preview.PreviewWrapper

@Preview
@Composable
fun AppTextButtonPreview() = PreviewWrapper { _ ->
    AppTextButton(onClick = {}) { AppText("Text Button") }
}

@Preview
@Composable
fun AppOutlinedButtonPreview() = PreviewWrapper { _ ->
    AppOutlinedButton(onClick = {}) { AppText("Outlined") }
}
