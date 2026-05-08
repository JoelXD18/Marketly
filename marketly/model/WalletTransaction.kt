package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletTransaction(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val amount: Double,
    val description: String,
    @SerialName("created_at") val createdAt: String? = null
)