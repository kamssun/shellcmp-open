package com.example.archshowcase.payment.model

sealed interface PaymentState {
    data object Idle : PaymentState
    data object Initializing : PaymentState
    data object Ready : PaymentState
    data object Processing : PaymentState
    data class Success(val outTradeNo: String) : PaymentState
    data object Cancelled : PaymentState
    data class Failed(val code: Int, val message: String) : PaymentState
}
