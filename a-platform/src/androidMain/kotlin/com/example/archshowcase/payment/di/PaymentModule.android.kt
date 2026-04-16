package com.example.archshowcase.payment.di

import com.example.archshowcase.payment.AndroidPaymentService
import com.example.archshowcase.payment.service.PaymentService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createPaymentPlatformModule(): Module = module {
    singleOf(::AndroidPaymentService) bind PaymentService::class
}
