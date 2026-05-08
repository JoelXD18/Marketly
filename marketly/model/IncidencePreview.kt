package com.ramos.marketly.model

data class IncidencePreview(
    val id: String,
    val orderId: String,
    val productId: String,
    val productTitle: String,
    val productImageUrl: String?,
    val buyerUsername: String,
    val sellerUsername: String,
    val buyerId: String,
    val sellerId: String,
    val description: String,
    val status: String,
    val createdAt: String?,
    val amount: Double
)