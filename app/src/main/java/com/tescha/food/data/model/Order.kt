package com.tescha.food.data.model

import java.time.Instant

data class OrderItem(
    val product: Product,
    val quantity: Int,
)

data class Order(
    val id: String,
    val userId: String,
    val merchantId: String,
    val items: List<OrderItem>,
    val deliveryAddress: String,
    val status: OrderStatus,
    val total: Double,
    val createdAt: Instant = Instant.now(),
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    ON_THE_WAY,
    DELIVERED,
    CANCELLED,
}
