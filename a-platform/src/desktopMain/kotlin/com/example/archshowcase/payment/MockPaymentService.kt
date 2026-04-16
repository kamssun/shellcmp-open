package com.example.archshowcase.payment

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.payment.model.PaymentConfig
import com.example.archshowcase.payment.model.PaymentMethod
import com.example.archshowcase.payment.model.PaymentState
import com.example.archshowcase.payment.model.ProductInfo
import com.example.archshowcase.payment.service.PaymentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockPaymentService : PaymentService {

    private val _stateFlow = MutableStateFlow<PaymentState>(PaymentState.Idle)
    override val stateFlow: StateFlow<PaymentState> = _stateFlow.asStateFlow()

    override fun initialize(config: PaymentConfig) {
        Log.d(TAG) { "Mock Payment initialized: scene=${config.scene}" }
        _stateFlow.value = PaymentState.Ready
    }

    override suspend fun purchase(product: ProductInfo, method: PaymentMethod): Result<String> {
        Log.d(TAG) { "Mock Payment purchase: product=${product.name}, method=$method" }
        _stateFlow.value = PaymentState.Processing
        val outTradeNo = "mock-trade-${System.currentTimeMillis()}"
        _stateFlow.value = PaymentState.Success(outTradeNo)
        return Result.success(outTradeNo)
    }

    override fun onResume() {
        Log.d(TAG) { "Mock Payment onResume" }
    }

    override fun logout() {
        Log.d(TAG) { "Mock Payment logout" }
    }

    override fun destroy() {
        Log.d(TAG) { "Mock Payment destroy" }
        _stateFlow.value = PaymentState.Idle
    }

    companion object {
        private const val TAG = "MockPaymentService"
    }
}
