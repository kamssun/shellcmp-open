package com.example.archshowcase.network

/**
 * API 路径常量
 */
object ApiRoutes {
    private const val BASE_URL = "https://api.example.com"

    /** Public demo API for network layer verification */
    const val USERS = "https://jsonplaceholder.typicode.com/users"

    // Demo endpoints showing the routing pattern
    const val ROOMS_SEND_MSG = "$BASE_URL/rooms/v1/send_msg"
    const val MEMBER_INFO = "$BASE_URL/account/v1/info"
    const val PAYMENT_PRODUCT_LIST = "$BASE_URL/payments/v1/products"
    const val PAYMENT_CONTRACT = "$BASE_URL/payments/v1/contract"
    const val PAYMENT_GOOGLE_RESULT = "$BASE_URL/payments/v1/google_result"
    const val PAYMENT_APPLE_RESULT = "$BASE_URL/payments/v1/apple_result"
}
