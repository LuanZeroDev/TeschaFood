package com.tescha.food.data.repository

import com.tescha.food.data.model.Order
import com.tescha.food.data.model.OrderItem
import com.tescha.food.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createOrder(
        userId: String,
        merchantId: String,
        items: List<OrderItem>,
        deliveryAddress: String,
        shippingFee: Double = 25.0,
    ): Order
    suspend fun getOrderById(id: String): Order?
    suspend fun getOrdersByUser(userId: String): List<Order>
    fun observeOrderStatus(orderId: String): Flow<OrderStatus>
    suspend fun cancelOrder(orderId: String): Order
}
