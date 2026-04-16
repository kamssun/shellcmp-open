package com.example.archshowcase.payment.service

import com.example.archshowcase.payment.model.PaymentConfig
import com.example.archshowcase.payment.model.PaymentMethod
import com.example.archshowcase.payment.model.PaymentState
import com.example.archshowcase.payment.model.ProductInfo
import kotlinx.coroutines.flow.StateFlow

interface PaymentService {
    val stateFlow: StateFlow<PaymentState>

    fun initialize(config: PaymentConfig)
    suspend fun purchase(product: ProductInfo, method: PaymentMethod): Result<String>
    fun onResume()
    fun logout()
    fun destroy()
}
