package com.tescha.food.data.repository.supabase

import com.tescha.food.data.model.Order
import com.tescha.food.data.model.OrderItem
import com.tescha.food.data.model.OrderStatus
import com.tescha.food.data.remote.dto.InsertOrderDto
import com.tescha.food.data.remote.dto.InsertOrderItemDto
import com.tescha.food.data.remote.dto.OrderDto
import com.tescha.food.data.remote.supabase
import com.tescha.food.data.repository.OrderRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.delay
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

// Punto fijo de entrega: campus TESCHA, Chalco
private const val TESCHA_LAT = 19.234477
private const val TESCHA_LNG = -98.840558

class SupabaseOrderRepository : OrderRepository {

    override suspend fun createOrder(
        userId: String,
        merchantId: String,
        items: List<OrderItem>,
        deliveryAddress: String,
        shippingFee: Double,
    ): Order {
        val subtotal = items.sumOf { it.product.price * it.quantity }

        val inserted = supabase.from("orders").insert(
            InsertOrderDto(
                clientId = userId,
                storeId = merchantId,
                status = "pagado",   // pago simulado confirmado (RF-08)
                deliveryLatitude = TESCHA_LAT,
                deliveryLongitude = TESCHA_LNG,
                subtotal = subtotal,
                shippingFee = shippingFee,
            )
        ) { select() }.decodeSingle<OrderDto>()

        val orderItems = items.map {
            InsertOrderItemDto(
                orderId = inserted.id,
                productId = it.product.id,
                quantity = it.quantity,
                unitPrice = it.product.price,
            )
        }
        supabase.from("order_items").insert(orderItems)

        return inserted.toDomain(items)
    }

    override suspend fun getOrderById(id: String): Order? {
        return supabase.from("orders")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<OrderDto>()
            ?.toDomain(emptyList())
    }

    override suspend fun getOrdersByUser(userId: String): List<Order> {
        return supabase.from("orders")
            .select { filter { eq("client_id", userId) } }
            .decodeList<OrderDto>()
            .map { it.toDomain(emptyList()) }
    }

    // RF-08: escucha cambios en tiempo real vía Realtime (RNF-08: asíncrono, no bloquea UI)
    override fun observeOrderStatus(orderId: String): Flow<OrderStatus> = flow {
        val channel = supabase.channel("order-$orderId")

        val changes = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "orders"
        }

        channel.subscribe()

        changes
            .filter { action -> action.record["id"]?.jsonPrimitive?.content == orderId }
            .mapNotNull { action -> action.record["status"]?.jsonPrimitive?.content?.toOrderStatus() }
            .collect { emit(it) }
    }

    override suspend fun cancelOrder(orderId: String): Order {
        val updated = supabase.from("orders").update(
            mapOf("status" to "cancelado")
        ) {
            filter { eq("id", orderId) }
            select()
        }.decodeSingle<OrderDto>()
        return updated.toDomain(emptyList())
    }

    private fun OrderDto.toDomain(items: List<OrderItem>) = Order(
        id = id,
        userId = clientId,
        merchantId = storeId,
        items = items,
        deliveryAddress = "",
        status = status.toOrderStatus(),
        total = (subtotal + shippingFee),
        createdAt = try { Instant.parse(createdAt) } catch (_: Exception) { Instant.now() },
    )

    private fun String.toOrderStatus() = when (this) {
        "carrito"          -> OrderStatus.PENDING
        "procesando_pago"  -> OrderStatus.PENDING
        "pagado"           -> OrderStatus.CONFIRMED
        "preparando"       -> OrderStatus.PREPARING
        "en_camino"        -> OrderStatus.ON_THE_WAY
        "entregado"        -> OrderStatus.DELIVERED
        "cancelado"        -> OrderStatus.CANCELLED
        else               -> OrderStatus.PENDING
    }
}
