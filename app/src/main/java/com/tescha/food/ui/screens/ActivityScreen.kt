package com.tescha.food.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.tescha.food.data.model.Order
import com.tescha.food.data.model.OrderStatus
import com.tescha.food.ui.components.DeliveryMap
import com.tescha.food.ui.components.TESCHA_DEST
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.ActiveOrderUi
import com.tescha.food.ui.viewmodel.ActivityViewModel
import com.tescha.food.ui.viewmodel.GhostDelivery

@Composable
fun ActivityScreen(activityViewModel: ActivityViewModel = viewModel()) {
    val orders      by activityViewModel.orders.collectAsState()
    val active      by activityViewModel.activeOrder.collectAsState()
    val ghost       by activityViewModel.ghost.collectAsState()
    val activeRoute by activityViewModel.activeRoute.collectAsState()
    val isLoading   by activityViewModel.isLoading.collectAsState()

    LaunchedEffect(active?.order?.id) {
        active?.order?.id?.let { activityViewModel.loadLastKnownLocation(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Actividad",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = { activityViewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = MetallicGold)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MetallicGold, strokeWidth = 2.dp)
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Repartidor fantasma (simulación post-pago)
            if (ghost != null) {
                item { GhostTrackingCard(ghost = ghost!!) }
            }

            // Nota: para pedidos en_camino reales (miki, etc.), el ActivityViewModel
            // ya lanza un GhostDelivery → la tarjeta de seguimiento se muestra arriba.

            if (orders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = OutlineVariant, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Sin pedidos aún", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Historial de pedidos",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(orders) { order -> OrderHistoryCard(order = order) }
            }
        }
    }
}

// ─── Mapa de seguimiento compartido: miniatura + pantalla completa al tocar ──

@Composable
private fun TrackingMapBox(
    storeLatLng: LatLng,
    repartidorPos: LatLng?,
    visitedRoute: List<LatLng>?,
    remainingRoute: List<LatLng>?,
) {
    var fullscreen by remember { mutableStateOf(false) }

    // Miniatura (dentro del card) — usa liteMode para cargar rápido y permitir clic
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .clickable { fullscreen = true },
    ) {
        DeliveryMap(
            modifier = Modifier.fillMaxSize(),
            origin = storeLatLng,
            repartidorLocation = repartidorPos,
            ghostVisited = visitedRoute,
            ghostRemaining = remainingRoute,
            zoom = 14f,
            liteMode = true,
        )

        // Botón "Ampliar" — clic explícito (encima del mapa en lite mode no intercepta)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { fullscreen = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text("Ampliar", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }

        if (repartidorPos == null && visitedRoute.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(SurfaceContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MetallicGold, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Localizando repartidor…", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
        }
    }

    // Diálogo pantalla completa
    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DeliveryMap(
                    modifier = Modifier.fillMaxSize(),
                    origin = storeLatLng,
                    repartidorLocation = repartidorPos,
                    ghostVisited = visitedRoute,
                    ghostRemaining = remainingRoute,
                    zoom = 16f,
                )
                // Botón cerrar
                IconButton(
                    onClick = { fullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}

// ─── GhostTrackingCard ────────────────────────────────────────────────────────

@Composable
private fun GhostTrackingCard(ghost: GhostDelivery) {
    val etaMin = ghost.etaSeconds / 60
    val etaSec = ghost.etaSeconds % 60
    val storeLatLng = ghost.routePoints.firstOrNull() ?: TESCHA_DEST

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(2.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, MetallicGold, Color.Transparent))
                )
            )

            AnimatedVisibility(visible = ghost.arrived, enter = fadeIn() + slideInVertically()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MetallicGold.copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "¡Tu pedido llegó al campus TESCHA! 🎉",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MetallicGold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        if (ghost.arrived) "Pedido entregado" else "Repartidor en camino",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Destino: Campus TESCHA, Chalco",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                }
                if (!ghost.arrived) {
                    EtaChip("${etaMin}m ${etaSec}s")
                } else {
                    DeliveredChip()
                }
            }

            TrackingMapBox(
                storeLatLng = storeLatLng,
                repartidorPos = ghost.currentPosition,
                visitedRoute = ghost.visitedPoints.takeIf { it.size >= 2 },
                remainingRoute = ghost.remainingPoints.takeIf { it.size >= 2 },
            )

            val progress = if (ghost.routePoints.size > 1)
                ghost.currentIndex.toFloat() / (ghost.routePoints.size - 1) else 1f
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Restaurante", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text("TESCHA", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MetallicGold,
                    trackColor = SurfaceContainerHigh,
                )
            }
        }
    }
}

// ─── LiveTrackingCard (pedidos en_camino reales, ej. miki) ───────────────────

@Composable
private fun LiveTrackingCard(active: ActiveOrderUi, fullRoute: List<LatLng>) {
    val storeLatLng = LatLng(19.2352, -98.8415)
    val repartidorPos = active.repartidorLocation

    // Si tenemos la ruta real, partirla por el punto más cercano al repartidor.
    val (visitedRoute, remainingRoute) = remember(fullRoute, repartidorPos) {
        if (fullRoute.size >= 2 && repartidorPos != null) {
            val idx = fullRoute.indices.minByOrNull {
                val p = fullRoute[it]
                val dLat = p.latitude - repartidorPos.latitude
                val dLng = p.longitude - repartidorPos.longitude
                dLat * dLat + dLng * dLng
            } ?: 0
            val visited = fullRoute.take(idx + 1) + repartidorPos
            val remaining = listOf(repartidorPos) + fullRoute.drop(idx + 1)
            visited to remaining
        } else if (fullRoute.size >= 2) {
            null to fullRoute
        } else if (repartidorPos != null) {
            listOf(storeLatLng, repartidorPos) to listOf(repartidorPos, TESCHA_DEST)
        } else {
            null to listOf(storeLatLng, TESCHA_DEST)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, MetallicGold.copy(alpha = 0.6f), Color.Transparent))
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Pedido en camino",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Actualización en tiempo real",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGold,
                    )
                }
                LiveChip()
            }

            TrackingMapBox(
                storeLatLng = storeLatLng,
                repartidorPos = repartidorPos,
                visitedRoute = visitedRoute,
                remainingRoute = remainingRoute,
            )
        }
    }
}

// ─── Chips de estado ──────────────────────────────────────────────────────────

@Composable
private fun EtaChip(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Burgundy.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(Icons.Default.Schedule, contentDescription = null, tint = Burgundy, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Burgundy)
    }
}

@Composable
private fun DeliveredChip() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
        Spacer(Modifier.width(5.dp))
        Text("Entregado", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4CAF50))
    }
}

@Composable
private fun LiveChip() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
        Spacer(Modifier.width(4.dp))
        Text("En vivo", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
    }
}

// ─── Historial ────────────────────────────────────────────────────────────────

@Composable
private fun OrderHistoryCard(order: Order) {
    val (statusColor, statusLabel, statusIcon) = orderStatusUi(order.status)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Pedido #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MetallicGold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${order.items.sumOf { it.quantity }} artículo(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }
        }
    }
}

private data class StatusUi(val color: Color, val label: String, val icon: ImageVector)

private fun orderStatusUi(status: OrderStatus): StatusUi = when (status) {
    OrderStatus.PENDING    -> StatusUi(MetallicGold,      "Procesando pago", Icons.Default.HourglassTop)
    OrderStatus.CONFIRMED  -> StatusUi(Color(0xFF4CAF50), "Pagado",          Icons.Default.CheckCircle)
    OrderStatus.PREPARING  -> StatusUi(Color(0xFF2196F3), "Preparando",      Icons.Default.Restaurant)
    OrderStatus.ON_THE_WAY -> StatusUi(Color(0xFF9C27B0), "En camino",       Icons.Default.DeliveryDining)
    OrderStatus.DELIVERED  -> StatusUi(MetallicGold,      "Entregado",       Icons.Default.TaskAlt)
    OrderStatus.CANCELLED  -> StatusUi(Error,             "Cancelado",       Icons.Default.Cancel)
}
