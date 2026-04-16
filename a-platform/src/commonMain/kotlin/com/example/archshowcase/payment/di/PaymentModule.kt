package com.example.archshowcase.payment.di

import com.example.archshowcase.core.AppRuntimeState
import com.example.archshowcase.core.di.featureModuleOf
import com.example.archshowcase.payment.api.DefaultPaymentApi
import com.example.archshowcase.payment.api.MockPaymentApi
import com.example.archshowcase.payment.api.PaymentApi
import com.example.archshowcase.payment.api.PaymentRepository
import com.example.archshowcase.payment.service.PaymentService
import org.koin.core.module.Module

expect fun createPaymentPlatformModule(): Module

private val paymentFeature = featureModuleOf<PaymentService> {
    includes(createPaymentPlatformModule())
    single<PaymentApi> { if (AppRuntimeState.isInPreview) MockPaymentApi() else DefaultPaymentApi() }
    single { PaymentRepository(get()) }
}

fun loadPaymentModule() = paymentFeature.load()
