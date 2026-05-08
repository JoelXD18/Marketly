package com.ramos.marketly.model

data class ChatPreview(
    val productId: String,
    val productTitle: String,
    val productImageUrl: String?,
    val lastMessage: String,
    val sellerId: String,
    val sellerUsername: String = "",
    val otherUsername: String = "",
    val otherUserRole: String = "cliente",
    val chatId: String = "",
    val otherUserId: String = ""
)