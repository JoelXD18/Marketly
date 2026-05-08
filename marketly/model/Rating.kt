package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewed_id") val reviewedId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)