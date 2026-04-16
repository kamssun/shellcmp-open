package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.background
import com.example.archshowcase.core.analytics.appClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_plus_album
import com.example.archshowcase.resources.chat_plus_camera

@Composable
fun PlusPanel(
    onPhotoClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PlusPanelItem(icon = "\uD83D\uDDBC", label = tr(Res.string.chat_plus_album), onClick = onPhotoClick)
            PlusPanelItem(icon = "\uD83D\uDCF7", label = tr(Res.string.chat_plus_camera), onClick = onCameraClick)
        }
    }
}

@Composable
private fun PlusPanelItem(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.appClickable(component = "PlusPanel", onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AppText(text = icon, style = AppTheme.typography.headlineMedium)
        }
        AppText(
            text = label,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview
@Composable
private fun PlusPanelPreview() {
    AppTheme {
        PlusPanel(onPhotoClick = {}, onCameraClick = {})
    }
}
