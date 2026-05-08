package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val role: String = "cliente",
    val balance: Double = 0.0,
    val status: String = "activo",
    @SerialName("created_at")
    val createdAt: String? = null
)