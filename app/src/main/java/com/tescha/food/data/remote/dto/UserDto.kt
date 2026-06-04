package com.tescha.food.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    @SerialName("full_name")  val fullName: String,
    val email: String,
    val role: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val balance: Double = 0.0,
)

@Serializable
data class DeliveryLocationDto(
    @SerialName("order_id")      val orderId: String,
    @SerialName("repartidor_id") val repartidorId: String,
    val latitude: Double,
    val longitude: Double,
)
