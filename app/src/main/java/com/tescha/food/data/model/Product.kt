package com.tescha.food.data.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String = "",
    val categoryId: String,
    val merchantId: String,
    val merchantName: String = "",
    val merchantLatitude: Double? = null,
    val merchantLongitude: Double? = null,
    val maxDeliveryKm: Double = 5.0,   // RF-01: radio máximo del vendedor
    val isAvailable: Boolean = true,
)
