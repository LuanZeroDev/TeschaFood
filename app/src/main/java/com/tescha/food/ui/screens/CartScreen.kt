package com.tescha.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.CartItem
import com.tescha.food.ui.viewmodel.CartSummary
import com.tescha.food.ui.viewmodel.CartViewModel

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    cartViewModel: CartViewModel,
) {
    val summary by cartViewModel.summary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Mi Carrito",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (summary.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = OutlineVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Tu carrito está vacío", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            // ── Lista de items ───────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(summary.items) { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { cartViewModel.increment(item.product.id) },
                        onDecrement = { cartViewModel.decrement(item.product.id) },
                    )
                }
            }

            // ── Desglose de costos (RF-05) ───────────────────────────────────
            CostBreakdown(summary = summary, onCheckout = onCheckout)
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // borde superior metálico — DESIGN.md
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, MetallicGold.copy(alpha = 0.4f), Color.Transparent))
                )
        )
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (item.product.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = item.product.imageUrl,
                        contentDescription = item.product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { CircularProgressIndicator(color = MetallicGold, strokeWidth = 1.dp, modifier = Modifier.size(20.dp)) },
                        error = { },
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$${String.format("%.2f", item.product.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MetallicGold,
                )
            }
            // Control de cantidad
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Quitar", tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                }
                Text(
                    "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Burgundy),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// RF-05: Desglose explícito de costos antes de habilitar el pago
@Composable
private fun CostBreakdown(summary: CartSummary, onCheckout: () -> Unit) {
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Resumen del pedido",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))

            // Dirección fija de entrega (campus TESCHA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainer)
                    .padding(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        "Punto de entrega",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGold,
                    )
                    Text(
                        "Carretera Federal México Cuautla s/n, La Candelaria\nTlapala, 56641 Chalco de Díaz Covarrubias, Méx.\n📍 19.234477, -98.840558",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            CostRow("Subtotal", summary.subtotal)

            if (summary.outOfRange) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ErrorContainer)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Fuera de rango de entrega (> 5 km)",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnErrorContainer,
                    )
                }
            } else {
                CostRow(
                    label = if (summary.distanceKm != null)
                        "Envío (${"%.1f".format(summary.distanceKm)} km)"
                    else "Envío",
                    amount = summary.shippingFee ?: 25.0,
                )
                Divider(color = OutlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "$${String.format("%.2f", summary.total)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MetallicGold,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                    enabled = summary.items.isNotEmpty() && !summary.outOfRange,
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Proceder al pago", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CostRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        Text("$${String.format("%.2f", amount)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}
