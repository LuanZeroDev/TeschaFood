package com.tescha.food.data.repository.stub

import com.tescha.food.data.model.Order
import com.tescha.food.data.model.OrderItem
import com.tescha.food.data.model.OrderStatus
import com.tescha.food.data.repository.OrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID

class StubOrderRepository : OrderRepository {

    private val orders = mutableListOf<Order>()

    override suspend fun createOrder(
        userId: String,
        merchantId: String,
        items: List<OrderItem>,
        deliveryAddress: String,
        shippingFee: Double,
    ): Order {
        val order = Order(
            id = UUID.randomUUID().toString(),
            userId = userId,
            merchantId = merchantId,
            items = items,
            deliveryAddress = deliveryAddress,
            status = OrderStatus.CONFIRMED,
            total = items.sumOf { it.product.price * it.quantity } + shippingFee,
            createdAt = Instant.now(),
        )
        orders.add(order)
        return order
    }

    override suspend fun getOrderById(id: String): Order? =
        orders.find { it.id == id }

    override suspend fun getOrdersByUser(userId: String): List<Order> =
        orders.filter { it.userId == userId }

    // Simula progresión de estados para desarrollo
    override fun observeOrderStatus(orderId: String): Flow<OrderStatus> = flow {
        val progression = listOf(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.ON_THE_WAY,
            OrderStatus.DELIVERED,
        )
        for (status in progression) {
            emit(status)
            delay(3_000)
        }
    }

    override suspend fun cancelOrder(orderId: String): Order {
        val index = orders.indexOfFirst { it.id == orderId }
        require(index >= 0) { "Order $orderId not found" }
        val cancelled = orders[index].copy(status = OrderStatus.CANCELLED)
        orders[index] = cancelled
        return cancelled
    }
}
