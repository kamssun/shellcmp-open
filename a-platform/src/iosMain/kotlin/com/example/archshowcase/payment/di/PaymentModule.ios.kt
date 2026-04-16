package com.example.archshowcase.payment.di

import com.example.archshowcase.payment.IosPaymentService
import com.example.archshowcase.payment.service.PaymentService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun createPaymentPlatformModule(): Module = module {
    singleOf(::IosPaymentService) bind PaymentService::class
}
