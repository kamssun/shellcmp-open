package com.example.archshowcase.presentation.demo.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.archshowcase.core.compose.OBOLaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.archshowcase.presentation.common.AsyncImage
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppScaffold
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.core.util.Log

private const val TAG = "DetailDemo"

@Composable
fun DetailContent(component: DetailComponent) {
    OBOLaunchedEffect(component.itemId) {
        Log.d(TAG) { "Loaded detail for: ${component.itemId}" }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = { AppText(tr(Res.string.title_detail, component.itemId), style = AppTheme.typography.titleMedium) },
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
            AppText(tr(Res.string.text_verify_route), style = AppTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppText(tr(Res.string.text_route_params), style = AppTheme.typography.labelMedium)
                    AppText(tr(Res.string.text_route_id, component.itemId), style = AppTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = "https://picsum.photos/seed/${component.itemId}/300/200",
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(onClick = { component.onBack() }) {
                AppText(tr(Res.string.btn_go_back))
            }
        }
    }
}

@Preview
@Composable
fun DetailContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultDetailComponent(componentContext, "preview_001") }
    DetailContent(component)
}
