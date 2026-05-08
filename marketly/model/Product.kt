package com.ramos.marketly.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SellerInfo(
    val username: String,
    val role: String = "cliente"
)

@Serializable
data class Product(
    val id: String,
    @SerialName("seller_id")
    val sellerId: String,
    val title: String,
    val description: String,
    val price: Double,
    @SerialName("product_type")
    val productType: String,
    @SerialName("file_url")
    val fileUrl: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String>? = null,
    val status: String = "activo",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("users")
    val seller: SellerInfo? = null
)