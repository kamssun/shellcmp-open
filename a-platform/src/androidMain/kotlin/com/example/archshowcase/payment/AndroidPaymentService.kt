// Stub implementation — SDK dependencies removed for open-source showcase
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

class AndroidPaymentService : PaymentService {

    private val _stateFlow = MutableStateFlow<PaymentState>(PaymentState.Idle)
    override val stateFlow: StateFlow<PaymentState> = _stateFlow.asStateFlow()

    override fun initialize(config: PaymentConfig) {
        _stateFlow.value = PaymentState.Initializing
        // Stub: Payment SDK not available
        Log.d(TAG) { "Stub: Payment SDK not initialized" }
        _stateFlow.value = PaymentState.Ready
    }

    override suspend fun purchase(product: ProductInfo, method: PaymentMethod): Result<String> {
        // Stub: Payment SDK not available
        return Result.failure(NotImplementedError("Payment SDK stub"))
    }

    override fun onResume() {
        // Stub: no-op
    }

    override fun logout() {
        // Stub: no-op
    }

    override fun destroy() {
        _stateFlow.value = PaymentState.Idle
    }

    companion object {
        private const val TAG = "AndroidPaymentService"
    }
}

class PaymentCancelledException : Exception("Payment cancelled by user")
