package com.example.archshowcase.payment

import com.example.archshowcase.core.util.Log
import com.example.archshowcase.getPaymentBridgeOrNull
import com.example.archshowcase.payment.model.PaymentConfig
import com.example.archshowcase.payment.model.PaymentMethod
import com.example.archshowcase.payment.model.PaymentState
import com.example.archshowcase.payment.model.ProductInfo
import com.example.archshowcase.payment.service.PaymentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosPaymentService : PaymentService {

    private val _stateFlow = MutableStateFlow<PaymentState>(PaymentState.Idle)
    override val stateFlow: StateFlow<PaymentState> = _stateFlow.asStateFlow()

    override fun initialize(config: PaymentConfig) {
        val bridge = getPaymentBridgeOrNull()
        if (bridge == null) {
            Log.w(TAG) { "PaymentBridge not set, skipping initialization" }
            _stateFlow.value = PaymentState.Failed(-1, "PaymentBridge not available")
            return
        }
        _stateFlow.value = PaymentState.Initializing
        bridge.initialize(scene = config.scene)
        _stateFlow.value = PaymentState.Ready
    }

    override suspend fun purchase(product: ProductInfo, method: PaymentMethod): Result<String> {
        val bridge = getPaymentBridgeOrNull()
            ?: return Result.failure(IllegalStateException("PaymentBridge not set"))

        _stateFlow.value = PaymentState.Processing

        return suspendCancellableCoroutine { cont ->
            bridge.purchase(
                productId = product.id,
                planId = product.planId,
                method = method.name,
                callback = object : PaymentBridgeCallback {
                    override fun onSuccess(outTradeNo: String) {
                        _stateFlow.value = PaymentState.Success(outTradeNo)
                        if (cont.isActive) cont.resume(Result.success(outTradeNo))
                    }

                    override fun onCancelled() {
                        _stateFlow.value = PaymentState.Cancelled
                        if (cont.isActive) cont.resume(Result.failure(RuntimeException("Payment cancelled")))
                    }

                    override fun onError(code: Int, message: String) {
                        _stateFlow.value = PaymentState.Failed(code, message)
                        if (cont.isActive) cont.resume(Result.failure(RuntimeException(message)))
                    }
                }
            )
        }
    }

    override fun onResume() {
        // iOS 不需要 onResume
    }

    override fun logout() {
        getPaymentBridgeOrNull()?.logout()
    }

    override fun destroy() {
        getPaymentBridgeOrNull()?.destroy()
        _stateFlow.value = PaymentState.Idle
    }

    companion object {
        private const val TAG = "IosPaymentService"
    }
}
