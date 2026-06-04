package com.tescha.food.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    @SerialName("store_id")   val storeId: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    @SerialName("image_url")  val imageUrl: String? = null,
    val available: Boolean,
)
