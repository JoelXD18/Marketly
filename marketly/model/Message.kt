package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("sender_id") val senderId: String,
    val message: String,
    @SerialName("message_type") val messageType: String = "normal",
    @SerialName("extra_data") val extraData: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)