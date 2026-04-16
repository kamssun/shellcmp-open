package com.example.archshowcase.payment.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentProductResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: PaymentProductData? = null,
)

@Serializable
data class PaymentProductData(
    @SerialName("pay_methods") val payMethods: List<PayMethodDto> = emptyList(),
)

@Serializable
data class PayMethodDto(
    @SerialName("name") val name: String,
    @SerialName("pay_method") val payMethod: String,
    @SerialName("products") val products: List<ProductDto> = emptyList(),
)

@Serializable
data class ProductDto(
    @SerialName("product_id") val productId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("price") val price: String,
    @SerialName("currency") val currency: String,
    @SerialName("sku_count") val skuCount: Int = 1,
    @SerialName("icon") val icon: String = "",
)

@Serializable
data class CreateOrderRequest(
    @SerialName("product_id") val productId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("scene") val scene: String,
)

@Serializable
data class CreateOrderResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: OrderData? = null,
)

@Serializable
data class OrderData(
    @SerialName("out_trade_no") val outTradeNo: String,
    @SerialName("product_id") val productId: String,
)

@Serializable
data class ReportResultRequest(
    @SerialName("out_trade_no") val outTradeNo: String,
    @SerialName("result_code") val resultCode: Int,
)

@Serializable
data class ReportResultResponse(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
)
