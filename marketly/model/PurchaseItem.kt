package com.ramos.marketly.model

data class PurchaseItem(
    val orderId: String,
    val productId: String,
    val productTitle: String,
    val productImageUrl: String?,
    val amount: Double,
    val status: String,
    val sellerId: String,
    val chatId: String
)