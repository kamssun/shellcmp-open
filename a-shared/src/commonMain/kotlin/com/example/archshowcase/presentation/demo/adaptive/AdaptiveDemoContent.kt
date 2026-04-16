package com.example.archshowcase.presentation.demo.adaptive

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.presentation.common.AsyncImage
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.btn_back
import com.example.archshowcase.resources.text_adaptive_features
import com.example.archshowcase.resources.text_feature_desc
import com.example.archshowcase.resources.text_mock_list_hint
import com.example.archshowcase.resources.text_verify_adaptive
import com.example.archshowcase.resources.title_adaptive_demo

private data class Conversation(val id: String, val title: String, val preview: String)

private val sampleConversations = listOf(
    Conversation("1", "Alice", "Hey, how are you?"),
    Conversation("2", "Bob", "Meeting at 3pm"),
    Conversation("3", "Charlie", "Check out this link..."),
)

@Composable
fun AdaptiveDemoContent(component: AdaptiveDemoComponent) {
    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_adaptive_demo), style = AppTheme.typography.titleMedium) },
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
            AppText(tr(Res.string.text_verify_adaptive), style = AppTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppText(tr(Res.string.text_feature_desc), style = AppTheme.typography.labelMedium)
                    AppText(
                        tr(Res.string.text_adaptive_features),
                        style = AppTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppText(tr(Res.string.text_mock_list_hint), style = AppTheme.typography.labelMedium)

            Spacer(modifier = Modifier.height(8.dp))

            sampleConversations.take(3).forEach { conversation ->
                AppCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = "https://picsum.photos/seed/${conversation.id}/50",
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            AppText(conversation.title, style = AppTheme.typography.titleSmall)
                            AppText(conversation.preview, style = AppTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AdaptiveDemoContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultAdaptiveDemoComponent(componentContext) }
    AdaptiveDemoContent(component)
}
