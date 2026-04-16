package com.example.archshowcase.payment.model

data class PaymentMethodGroup(
    val name: String,
    val payMethod: PaymentMethod,
    val products: List<ProductInfo>,
)
