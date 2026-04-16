package com.example.archshowcase.payment.api

import com.example.archshowcase.payment.model.OrderInfo
import com.example.archshowcase.payment.model.PaymentMethod
import com.example.archshowcase.payment.model.PaymentMethodGroup
import com.example.archshowcase.payment.model.ProductInfo

class PaymentRepository(private val api: PaymentApi) {

    suspend fun getProducts(scene: String, country: String): Result<List<PaymentMethodGroup>> =
        api.getProducts(scene, country).mapCatching { response ->
            if (response.code != 0) error(response.message)
            response.data?.payMethods?.map { it.toDomain() } ?: emptyList()
        }

    suspend fun createOrder(productId: String, planId: String, scene: String): Result<OrderInfo> =
        api.createOrder(CreateOrderRequest(productId, planId, scene)).mapCatching { response ->
            if (response.code != 0) error(response.message)
            val data = response.data ?: error("Empty order data")
            OrderInfo(outTradeNo = data.outTradeNo, productId = data.productId)
        }

    suspend fun reportResult(outTradeNo: String, resultCode: Int, platform: String): Result<Unit> {
        val request = ReportResultRequest(outTradeNo, resultCode)
        val result = when (platform) {
            "google" -> api.reportGooglePayResult(request)
            "apple" -> api.reportApplePayResult(request)
            else -> api.reportGooglePayResult(request)
        }
        return result.mapCatching { response ->
            if (response.code != 0) error(response.message)
        }
    }
}

private fun PayMethodDto.toDomain(): PaymentMethodGroup = PaymentMethodGroup(
    name = name,
    payMethod = when (payMethod) {
        "GOOGLE_PAY" -> PaymentMethod.GOOGLE_PAY
        "CUSTOM_PAY" -> PaymentMethod.CUSTOM_PAY
        "APPLE_IAP" -> PaymentMethod.APPLE_IAP
        else -> PaymentMethod.CUSTOM_PAY
    },
    products = products.map { it.toDomain() },
)

private fun ProductDto.toDomain(): ProductInfo = ProductInfo(
    id = productId,
    planId = planId,
    name = productName,
    price = price,
    currency = currency,
    skuCount = skuCount,
    icon = icon,
)
