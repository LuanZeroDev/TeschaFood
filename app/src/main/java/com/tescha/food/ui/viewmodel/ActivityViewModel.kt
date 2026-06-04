package com.tescha.food.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.R
import com.tescha.food.data.model.Order
import com.tescha.food.data.model.OrderStatus
import com.tescha.food.data.remote.DirectionsService
import com.tescha.food.data.remote.dto.DeliveryLocationDto
import com.tescha.food.data.remote.supabase
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double

// Punto de entrega fijo: campus TESCHA, Chalco
private val TESCHA_LATLNG = LatLng(19.234477, -98.840558)
// Tienda demo alejada ~2 km del campus para que la ruta sea visible en el mapa.
val DEMO_STORE = LatLng(19.252800, -98.857200)
private const val GHOST_STEPS = 15        // pasos de la simulación
private const val GHOST_STEP_MS = 8_000L  // 8 s por paso → ~2 min total
private const val NOTIF_CHANNEL = "delivery"

data class ActiveOrderUi(
    val order: Order,
    val repartidorLocation: LatLng?,
)

data class GhostDelivery(
    val routePoints: List<LatLng>,   // ruta completa tienda → TESCHA
    val currentIndex: Int,           // punto actual del repartidor
    val etaSeconds: Int,             // segundos restantes estimados
    val arrived: Boolean,
) {
    val currentPosition: LatLng get() = routePoints.getOrElse(currentIndex) { TESCHA_LATLNG }
    val visitedPoints: List<LatLng> get() = routePoints.take(currentIndex + 1)
    val remainingPoints: List<LatLng> get() = routePoints.drop(currentIndex)
}

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = (application as FoodApp).container.orderRepository
    private val sessionManager   = (application as FoodApp).container.sessionManager

    private val _orders   = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _activeOrder = MutableStateFlow<ActiveOrderUi?>(null)
    val activeOrder: StateFlow<ActiveOrderUi?> = _activeOrder.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ghost = MutableStateFlow<GhostDelivery?>(null)
    val ghost: StateFlow<GhostDelivery?> = _ghost.asStateFlow()

    private val _activeRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val activeRoute: StateFlow<List<LatLng>> = _activeRoute.asStateFlow()

    private var ghostJob: Job? = null

    init {
        setupNotificationChannel()
        loadOrders()
    }

    // ── Repartidor fantasma ─────────────────────────────────────────────────

    fun startGhostDelivery(storeLat: Double, storeLng: Double) {
        if (_ghost.value != null) return
        ghostJob?.cancel()

        val origin = LatLng(storeLat, storeLng)

        ghostJob = viewModelScope.launch {
            // 1. Intentar ruta real de Google Directions. Fallback: línea con waypoints.
            val realRoute = DirectionsService.getRoute(origin, TESCHA_LATLNG)
            val route = if (!realRoute.isNullOrEmpty() && realRoute.size >= 2) {
                resample(realRoute, GHOST_STEPS)
            } else {
                buildRoute(origin, TESCHA_LATLNG, GHOST_STEPS)
            }

            val totalSeconds = ((route.size - 1) * GHOST_STEP_MS / 1000).toInt()
            _ghost.value = GhostDelivery(route, 0, totalSeconds, false)

            for (step in 1 until route.size) {
                delay(GHOST_STEP_MS)
                val remaining = ((route.size - 1 - step) * GHOST_STEP_MS / 1000).toInt()
                val arrived = step == route.size - 1
                _ghost.value = GhostDelivery(route, step, remaining, arrived)
                if (arrived) sendArrivalNotification()
            }
        }
    }

    // Re-muestrea una polyline cualquiera a [steps] puntos equiespaciados por distancia.
    private fun resample(poly: List<LatLng>, steps: Int): List<LatLng> {
        if (poly.size <= steps) return poly
        // Calcula distancias acumuladas
        val cum = DoubleArray(poly.size)
        for (i in 1 until poly.size) {
            val a = poly[i - 1]; val b = poly[i]
            cum[i] = cum[i - 1] + haversine(a, b)
        }
        val total = cum.last()
        if (total <= 0.0) return poly
        val result = mutableListOf<LatLng>()
        for (s in 0 until steps) {
            val target = total * s / (steps - 1)
            // Encuentra segmento que contiene target
            var idx = cum.indexOfFirst { it >= target }
            if (idx <= 0) idx = 1
            val segStart = cum[idx - 1]
            val segEnd = cum[idx]
            val t = if (segEnd > segStart) (target - segStart) / (segEnd - segStart) else 0.0
            val a = poly[idx - 1]; val b = poly[idx]
            result.add(LatLng(
                a.latitude + (b.latitude - a.latitude) * t,
                a.longitude + (b.longitude - a.longitude) * t,
            ))
        }
        return result
    }

    private fun haversine(a: LatLng, b: LatLng): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val s = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) *
            Math.sin(dLng / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(s))
    }

    fun clearGhost() {
        ghostJob?.cancel()
        _ghost.value = null
    }

    // Fallback estilo "Manhattan": ruta con tramos alternados horizontales/verticales
    // que simula una trayectoria por calles en cuadrícula cuando Directions API falla.
    private fun buildRoute(origin: LatLng, dest: LatLng, steps: Int): List<LatLng> {
        val turns = 5  // número de tramos rectos (mientras más, más zig-zag por calles)
        val waypoints = mutableListOf<LatLng>(origin)
        for (i in 1 until turns) {
            val t = i.toDouble() / turns
            // Alternamos: en pasos impares movemos primero en longitud (horizontal),
            // en pares en latitud (vertical). Esto produce el patrón en escalera.
            val lat = if (i % 2 == 0)
                origin.latitude + (dest.latitude - origin.latitude) * t
            else
                origin.latitude + (dest.latitude - origin.latitude) * ((i - 1).toDouble() / turns)
            val lng = if (i % 2 == 0)
                origin.longitude + (dest.longitude - origin.longitude) * ((i - 1).toDouble() / turns)
            else
                origin.longitude + (dest.longitude - origin.longitude) * t
            waypoints.add(LatLng(lat, lng))
        }
        waypoints.add(dest)

        val result = mutableListOf<LatLng>()
        val segSteps = (steps / (waypoints.size - 1)).coerceAtLeast(1)
        for (i in 0 until waypoints.size - 1) {
            val from = waypoints[i]; val to = waypoints[i + 1]
            for (s in 0 until segSteps) {
                val t = s.toDouble() / segSteps
                result.add(LatLng(
                    from.latitude + (to.latitude - from.latitude) * t,
                    from.longitude + (to.longitude - from.longitude) * t,
                ))
            }
        }
        result.add(dest)
        return result
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(NOTIF_CHANNEL, "Entregas", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Notificaciones del repartidor"
            (getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun sendArrivalNotification() {
        val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(getApplication(), NOTIF_CHANNEL)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle("¡Tu pedido llegó! 🎉")
            .setContentText("El repartidor está en el campus TESCHA.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, notif)
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val userId = sessionManager.userId ?: return@runCatching
                val list = orderRepository.getOrdersByUser(userId)
                _orders.value = list

                val enCamino = list.firstOrNull { it.status == OrderStatus.ON_THE_WAY }
                if (enCamino != null) {
                    _activeOrder.value = ActiveOrderUi(enCamino, null)
                    // El seguimiento es totalmente simulado (repartidor fantasma).
                    // Lanzamos un ghost desde la tienda demo (alejada ~2 km del campus)
                    // para que la ruta por carretera sea visible en el mapa.
                    val storeLatLng = DEMO_STORE
                    startGhostDelivery(storeLatLng.latitude, storeLatLng.longitude)
                }
            }
            _isLoading.value = false
        }
    }

    // RF-11: sincronización vía Realtime
    private fun observeDeliveryLocation(orderId: String) {
        viewModelScope.launch {
            runCatching {
                val channel = supabase.channel("delivery-$orderId")

                val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "delivery_locations"
                }

                channel.subscribe()

                flow.collect { action ->
                    val record = action.record
                    if (record["order_id"]?.jsonPrimitive?.content == orderId) {
                        val lat = record["latitude"]?.jsonPrimitive?.double ?: return@collect
                        val lng = record["longitude"]?.jsonPrimitive?.double ?: return@collect
                        _activeOrder.value = _activeOrder.value?.copy(
                            repartidorLocation = LatLng(lat, lng)
                        )
                    }
                }
            } // runCatching
        }
    }

    // Carga la última ubicación conocida del repartidor para el pedido activo
    fun loadLastKnownLocation(orderId: String) {
        viewModelScope.launch {
            runCatching {
                val rows = supabase.from("delivery_locations")
                    .select {
                        filter { eq("order_id", orderId) }
                        limit(1)
                    }
                    .decodeList<DeliveryLocationDto>()

                rows.firstOrNull()?.let { loc ->
                    _activeOrder.value = _activeOrder.value?.copy(
                        repartidorLocation = LatLng(loc.latitude, loc.longitude)
                    )
                }
            }
        }
    }

    fun refresh() { loadOrders() }
}
