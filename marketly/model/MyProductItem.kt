package com.ramos.marketly.model

data class MyProductItem(
    val id: String,
    val title: String,
    val category: String,
    val price: Double,
    val imageUrl: String?,
    val status: String
)