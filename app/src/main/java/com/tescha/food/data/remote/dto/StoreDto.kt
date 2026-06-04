package com.tescha.food.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreDto(
    val id: String,
    @SerialName("owner_id")   val ownerId: String,
    val name: String,
    val description: String? = null,
    @SerialName("logo_url")   val logoUrl: String? = null,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("open_time")         val openTime: String? = null,
    @SerialName("close_time")        val closeTime: String? = null,
    @SerialName("max_delivery_km")   val maxDeliveryKm: Double,
)
