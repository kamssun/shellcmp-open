package com.example.archshowcase.payment.api

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.network.ApiRoutes
import kotlin.random.Random
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface PaymentApi {
    suspend fun getProducts(scene: String, country: String): Result<PaymentProductResponse>
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse>
    suspend fun reportGooglePayResult(request: ReportResultRequest): Result<ReportResultResponse>
    suspend fun reportApplePayResult(request: ReportResultRequest): Result<ReportResultResponse>
}

class DefaultPaymentApi : PaymentApi, KoinComponent {

    private val client: HttpClient by inject()

    override suspend fun getProducts(scene: String, country: String): Result<PaymentProductResponse> =
        runCatching {
            Log.d(TAG) { "Fetching products: scene=$scene, country=$country" }
            client.get(ApiRoutes.PAYMENT_PRODUCT_LIST) {
                parameter("scene", scene)
                if (country.isNotEmpty()) parameter("country", country)
            }.body<PaymentProductResponse>()
        }.onFailure { e ->
            Log.e(TAG, e) { "Failed to fetch products" }
        }

    override suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> =
        runCatching {
            Log.d(TAG) { "Creating order: productId=${request.productId}" }
            client.post(ApiRoutes.PAYMENT_CONTRACT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<CreateOrderResponse>()
        }.onFailure { e ->
            Log.e(TAG, e) { "Failed to create order" }
        }

    override suspend fun reportGooglePayResult(request: ReportResultRequest): Result<ReportResultResponse> =
        runCatching {
            Log.d(TAG) { "Reporting Google Pay result: ${request.outTradeNo}" }
            client.post(ApiRoutes.PAYMENT_GOOGLE_RESULT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ReportResultResponse>()
        }.onFailure { e ->
            Log.e(TAG, e) { "Failed to report Google Pay result" }
        }

    override suspend fun reportApplePayResult(request: ReportResultRequest): Result<ReportResultResponse> =
        runCatching {
            Log.d(TAG) { "Reporting Apple Pay result: ${request.outTradeNo}" }
            client.post(ApiRoutes.PAYMENT_APPLE_RESULT) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ReportResultResponse>()
        }.onFailure { e ->
            Log.e(TAG, e) { "Failed to report Apple Pay result" }
        }

    companion object {
        private const val TAG = "PaymentApi"
    }
}

class MockPaymentApi : PaymentApi {

    override suspend fun getProducts(scene: String, country: String): Result<PaymentProductResponse> =
        Result.success(
            PaymentProductResponse(
                code = 0,
                message = "success",
                data = PaymentProductData(
                    payMethods = listOf(
                        PayMethodDto(
                            name = "Google Pay",
                            payMethod = "GOOGLE_PAY",
                            products = listOf(
                                ProductDto("prod_001", "plan_basic", "Basic Plan", "9.99", "USD", 1, ""),
                                ProductDto("prod_002", "plan_pro", "Pro Plan", "19.99", "USD", 1, ""),
                            )
                        ),
                        PayMethodDto(
                            name = "Custom Pay",
                            payMethod = "CUSTOM_PAY",
                            products = listOf(
                                ProductDto("prod_003", "plan_coins_100", "100 Coins", "4.99", "USD", 100, ""),
                                ProductDto("prod_004", "plan_coins_500", "500 Coins", "19.99", "USD", 500, ""),
                            )
                        ),
                    )
                )
            )
        )

    override suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> =
        Result.success(
            CreateOrderResponse(
                code = 0,
                message = "success",
                data = OrderData(
                    outTradeNo = "mock-order-${Random.nextInt(100000, 999999)}",
                    productId = request.productId
                )
            )
        )

    override suspend fun reportGooglePayResult(request: ReportResultRequest): Result<ReportResultResponse> =
        Result.success(ReportResultResponse(code = 0, message = "success"))

    override suspend fun reportApplePayResult(request: ReportResultRequest): Result<ReportResultResponse> =
        Result.success(ReportResultResponse(code = 0, message = "success"))
}
