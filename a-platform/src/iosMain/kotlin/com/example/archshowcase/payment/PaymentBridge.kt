package com.example.archshowcase.payment

interface PaymentBridgeCallback {
    fun onSuccess(outTradeNo: String)
    fun onCancelled()
    fun onError(code: Int, message: String)
}

interface PaymentBridge {
    fun initialize(scene: String)
    fun purchase(productId: String, planId: String, method: String, callback: PaymentBridgeCallback)
    fun logout()
    fun destroy()
}
