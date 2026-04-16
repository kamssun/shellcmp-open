@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.archshowcase.presentation.chat.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.theme.AppTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

fun shouldShowTimeSeparator(currentTimestamp: Long, previousTimestamp: Long?): Boolean {
    if (previousTimestamp == null) return true
    return (currentTimestamp - previousTimestamp) >= FIVE_MINUTES_MS
}

@Composable
fun TimeSeparator(timestamp: Long) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val timeText = remember(timestamp, currentDate) { formatChatTime(timestamp) }
        AppText(
            text = timeText,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant
        )
    }
}

private fun formatChatTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val h = local.hour.toString().padStart(2, '0')
    val m = local.minute.toString().padStart(2, '0')

    return if (local.date == now.date) {
        "$h:$m"
    } else {
        "${local.monthNumber}月${local.dayOfMonth}日 $h:$m"
    }
}

@Preview
@Composable
private fun TimeSeparatorPreview() {
    AppTheme {
        TimeSeparator(timestamp = Clock.System.now().toEpochMilliseconds())
    }
}
