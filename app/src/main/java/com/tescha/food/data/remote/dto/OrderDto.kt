package com.tescha.food.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("client_id")            val clientId: String,
    @SerialName("store_id")             val storeId: String,
    val status: String,
    @SerialName("delivery_latitude")    val deliveryLatitude: Double,
    @SerialName("delivery_longitude")   val deliveryLongitude: Double,
    @SerialName("distance_km")          val distanceKm: Double? = null,
    @SerialName("estimated_minutes")    val estimatedMinutes: Int? = null,
    val subtotal: Double,
    @SerialName("shipping_fee")         val shippingFee: Double,
    val total: Double? = null,
    @SerialName("card_holder")          val cardHolder: String? = null,
    @SerialName("card_last4")           val cardLast4: String? = null,
    @SerialName("card_expiry")          val cardExpiry: String? = null,
    @SerialName("payment_initiated_at") val paymentInitiatedAt: String? = null,
    @SerialName("payment_confirmed_at") val paymentConfirmedAt: String? = null,
    @SerialName("created_at")           val createdAt: String? = null,
)

@Serializable
data class OrderItemDto(
    val id: String? = null,
    @SerialName("order_id")    val orderId: String,
    @SerialName("product_id")  val productId: String,
    val quantity: Int,
    @SerialName("unit_price")  val unitPrice: Double,
    val subtotal: Double? = null,
)

@Serializable
data class InsertOrderDto(
    @SerialName("client_id")           val clientId: String,
    @SerialName("store_id")            val storeId: String,
    val status: String = "carrito",
    @SerialName("delivery_latitude")   val deliveryLatitude: Double,
    @SerialName("delivery_longitude")  val deliveryLongitude: Double,
    @SerialName("distance_km")         val distanceKm: Double? = null,
    @SerialName("estimated_minutes")   val estimatedMinutes: Int? = null,
    val subtotal: Double,
    @SerialName("shipping_fee")        val shippingFee: Double,
)

@Serializable
data class InsertOrderItemDto(
    @SerialName("order_id")   val orderId: String,
    @SerialName("product_id") val productId: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
)
