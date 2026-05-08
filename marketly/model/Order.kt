package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    @SerialName("product_id")
    val productId: String,
    @SerialName("buyer_id")
    val buyerId: String,
    val status: String = "en_espera",
    val amount: Double,
    @SerialName("delivery_deadline")
    val deliveryDeadline: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("completed_at")
    val completedAt: String? = null
)