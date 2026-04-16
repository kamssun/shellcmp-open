package com.example.archshowcase.presentation.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppText

@Composable
fun LiveContent(
    component: LiveComponent,
    modifier: Modifier = Modifier,
) {
    val rtcState by component.rtcState.collectAsState()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppText(text = tr(Res.string.text_live_rtc, rtcState))
    }
}
