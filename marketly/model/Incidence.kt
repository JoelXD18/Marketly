package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Incidence(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("opened_by") val openedBy: String,
    @SerialName("resolved_by") val resolvedBy: String? = null,
    val description: String,
    val status: String = "abierta",
    val resolution: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)