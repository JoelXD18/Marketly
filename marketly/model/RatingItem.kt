package com.ramos.marketly.model

data class RatingItem(
    val rating: Int,
    val comment: String?,
    val reviewerUsername: String
)