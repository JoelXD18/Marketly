package com.ramos.marketly.model

data class SaleItem(
    val orderId: String,
    val productId: String,
    val productTitle: String,
    val productImageUrl: String?,
    val amount: Double,
    val status: String,
    val buyerId: String,
    val buyerUsername: String,
    val chatId: String
)