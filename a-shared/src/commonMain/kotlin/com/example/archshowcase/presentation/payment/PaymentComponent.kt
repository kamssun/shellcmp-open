package com.example.archshowcase.presentation.payment

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume
import com.example.archshowcase.core.scheduler.oboLaunch
import com.example.archshowcase.core.util.Log
import com.example.archshowcase.payment.api.PaymentRepository
import com.example.archshowcase.payment.di.loadPaymentModule
import com.example.archshowcase.payment.model.PaymentConfig
import com.example.archshowcase.payment.model.PaymentMethod
import com.example.archshowcase.payment.model.PaymentMethodGroup
import com.example.archshowcase.payment.model.PaymentState
import com.example.archshowcase.payment.model.ProductInfo
import com.example.archshowcase.payment.service.PaymentService
import com.example.archshowcase.presentation.navigation.AppComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface PaymentComponent {
    val paymentState: StateFlow<PaymentState>
    val products: StateFlow<List<PaymentMethodGroup>>
    val isLoading: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>

    fun onBack()
    fun loadProducts(scene: String)
    fun purchase(product: ProductInfo, method: PaymentMethod)
    fun dismissError()
}

class DefaultPaymentComponent(
    context: AppComponentContext,
) : PaymentComponent, AppComponentContext by context, KoinComponent {

    init {
        loadPaymentModule()
    }

    private val paymentService: PaymentService by inject()
    private val paymentRepository: PaymentRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val paymentState: StateFlow<PaymentState> = paymentService.stateFlow

    private val _products = MutableStateFlow<List<PaymentMethodGroup>>(emptyList())
    override val products: StateFlow<List<PaymentMethodGroup>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        paymentService.initialize(PaymentConfig(scene = "default"))

        lifecycle.doOnResume { paymentService.onResume() }
        lifecycle.doOnDestroy {
            paymentService.destroy()
            scope.cancel()
        }
    }

    override fun loadProducts(scene: String) {
        _isLoading.value = true
        _errorMessage.value = null
        scope.oboLaunch(OBO_TAG) {
            paymentRepository.getProducts(scene, country = "").fold(
                onSuccess = { groups ->
                    _products.value = groups
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    Log.e(TAG) { "Failed to load products: ${error.message}" }
                }
            )
        }
    }

    override fun purchase(product: ProductInfo, method: PaymentMethod) {
        _errorMessage.value = null
        scope.oboLaunch(OBO_TAG) {
            paymentService.purchase(product, method).fold(
                onSuccess = { outTradeNo ->
                    Log.d(TAG) { "Payment success: $outTradeNo" }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                    Log.e(TAG) { "Payment failed: ${error.message}" }
                }
            )
        }
    }

    override fun dismissError() {
        _errorMessage.value = null
    }

    companion object {
        private const val TAG = "PaymentComponent"
        private const val OBO_TAG = "PaymentComponent"
    }
}
