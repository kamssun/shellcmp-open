package com.example.archshowcase.payment.model

data class ProductInfo(
    val id: String,
    val planId: String,
    val name: String,
    val price: String,
    val currency: String,
    val skuCount: Int,
    val icon: String,
)
