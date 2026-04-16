package com.example.archshowcase.presentation.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.example.archshowcase.core.compose.exposure.ExposureLazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.archshowcase.resources.*
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.payment.model.PaymentState
import com.example.archshowcase.payment.model.ProductInfo
import com.example.archshowcase.presentation.component.AppButton
import com.example.archshowcase.presentation.component.AppCard
import com.example.archshowcase.presentation.component.AppCircularProgress
import com.example.archshowcase.presentation.component.AppIconButton
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.theme.AppTheme

@Composable
fun PaymentContent(
    component: PaymentComponent,
    modifier: Modifier = Modifier,
) {
    val paymentState by component.paymentState.collectAsState()
    val products by component.products.collectAsState()
    val isLoading by component.isLoading.collectAsState()
    val errorMessage by component.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        component.loadProducts("default")
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = { AppText(tr(Res.string.title_payment), style = AppTheme.typography.titleMedium) },
            navigationIcon = {
                AppIconButton(onClick = { component.onBack() }) {
                    AppText("<")
                }
            },
            actions = {
                PaymentStateIndicator(paymentState)
            }
        )

        errorMessage?.let { message ->
            AppCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                containerColor = AppTheme.colors.errorContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = message,
                        color = AppTheme.colors.onErrorContainer,
                        style = AppTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    AppTextButton(onClick = { component.dismissError() }) {
                        AppText(tr(Res.string.btn_dismiss))
                    }
                }
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppCircularProgress()
                }
            }
            else -> {
                ExposureLazyColumn(
                    listId = "payment_products",
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    products.forEach { group ->
                        item(contentType = "group_header") {
                            AppText(
                                text = group.name,
                                style = AppTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(
                            items = group.products,
                            key = { it.id },
                            contentType = { "product_card" }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onBuy = { component.purchase(product, group.payMethod) },
                                enabled = paymentState !is PaymentState.Processing,
                            )
                        }
                    }
                    item(contentType = "spacer") { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductInfo,
    onBuy: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = product.name,
                    style = AppTheme.typography.bodyLarge,
                )
                AppText(
                    text = "${product.currency} ${product.price}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AppButton(onClick = onBuy, enabled = enabled) {
                AppText(tr(Res.string.btn_buy))
            }
        }
    }
}

@Composable
private fun PaymentStateIndicator(state: PaymentState) {
    val text = when (state) {
        PaymentState.Idle -> tr(Res.string.text_payment_idle)
        PaymentState.Initializing -> tr(Res.string.text_payment_init)
        PaymentState.Ready -> tr(Res.string.text_payment_ready)
        PaymentState.Processing -> tr(Res.string.text_payment_processing)
        is PaymentState.Success -> tr(Res.string.text_payment_success)
        PaymentState.Cancelled -> tr(Res.string.text_payment_cancelled)
        is PaymentState.Failed -> tr(Res.string.text_payment_failed)
    }
    AppText(
        text = text,
        style = AppTheme.typography.labelSmall,
        modifier = Modifier.padding(end = 16.dp),
    )
}
