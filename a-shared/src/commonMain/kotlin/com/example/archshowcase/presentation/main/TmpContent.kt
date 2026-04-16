package com.example.archshowcase.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme

/** 占位页：纯色背景 + 居中白色大字 tab 名称 */
@Composable
fun TmpContent(tabName: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(color),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = tabName,
            style = AppTheme.typography.headlineLarge,
            color = Color.White,
        )
    }
}
