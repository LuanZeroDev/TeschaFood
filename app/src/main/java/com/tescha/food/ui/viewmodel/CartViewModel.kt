package com.tescha.food.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tescha.food.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

data class CartItem(val product: Product, val quantity: Int)

data class CartSummary(
    val items: List<CartItem>,
    val subtotal: Double,
    val distanceKm: Double?,
    val shippingFee: Double?,      // null = fuera de rango (RF-06 > 5km)
    val outOfRange: Boolean,
    val total: Double,
)

// Punto fijo de entrega: campus TESCHA, Chalco
private const val TESCHA_LAT = 19.234477
private const val TESCHA_LNG = -98.840558

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    private val _deliveryDistanceKm = MutableStateFlow<Double?>(null)

    private val _summary = MutableStateFlow(CartSummary(emptyList(), 0.0, null, null, false, 0.0))
    val summary: StateFlow<CartSummary> = _summary.asStateFlow()

    init {
        recalculate()
    }

    fun setDeliveryDistance(km: Double) {
        _deliveryDistanceKm.value = km
        recalculate()
    }

    fun addProduct(product: Product) {
        val current = _items.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            current[idx] = current[idx].copy(quantity = current[idx].quantity + 1)
        } else {
            current.add(CartItem(product, 1))
        }
        _items.value = current
        // Calcular distancia desde la tienda al punto fijo de entrega TESCHA
        if (_deliveryDistanceKm.value == null) {
            val mLat = product.merchantLatitude
            val mLng = product.merchantLongitude
            if (mLat != null && mLng != null) {
                _deliveryDistanceKm.value = haversineKm(mLat, mLng, TESCHA_LAT, TESCHA_LNG)
            }
        }
        recalculate()
    }

    fun increment(productId: String) {
        _items.value = _items.value.map {
            if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
        }
        recalculate()
    }

    fun decrement(productId: String) {
        _items.value = _items.value
            .map { if (it.product.id == productId) it.copy(quantity = it.quantity - 1) else it }
            .filter { it.quantity > 0 }
        recalculate()
    }

    fun clear() {
        _items.value = emptyList()
        _deliveryDistanceKm.value = null
        recalculate()
    }

    val itemCount: Int get() = _items.value.sumOf { it.quantity }

    private fun recalculate() {
        val items = _items.value
        val subtotal = items.sumOf { it.product.price * it.quantity }
        val km = _deliveryDistanceKm.value
        val shippingFee = calcShippingFee(km)
        val outOfRange = km != null && km > 5.0
        _summary.value = CartSummary(
            items = items,
            subtotal = subtotal,
            distanceKm = km,
            shippingFee = shippingFee,
            outOfRange = outOfRange,
            total = subtotal + (shippingFee ?: 0.0),
        )
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // RF-06: lógica de tarifa de envío (misma que función SQL calc_shipping_fee)
    private fun calcShippingFee(km: Double?): Double? {
        km ?: return 25.0   // sin GPS = tarifa base por defecto
        return when {
            km <= 2.0 -> 25.00
            km <= 5.0 -> Math.round((25.0 + (km - 2.0) * 10.0) * 100) / 100.0
            else      -> null  // fuera de rango
        }
    }
}
