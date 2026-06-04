package com.tescha.food.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.CartSummary
import com.tescha.food.ui.viewmodel.CheckoutViewModel
import com.tescha.food.ui.viewmodel.PaymentState
import kotlinx.coroutines.delay

// RF-08: pantalla de transición de estado asíncrono
// Muestra "Procesando Pago" → exactamente 4 s → "Pagado"
@Composable
fun PaymentResultScreen(
    summary: CartSummary,
    checkoutViewModel: CheckoutViewModel,
    onFinish: () -> Unit,
    onError: () -> Unit = {},
) {
    val paymentState by checkoutViewModel.paymentState.collectAsState()
    val errorMessage by checkoutViewModel.errorMessage.collectAsState()

    val isConfirmed = paymentState == PaymentState.CONFIRMED
    val isError = paymentState == PaymentState.ERROR

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isConfirmed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            if (isError) {
                // ── Estado: Error de pago ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .border(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("!", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "Pago no completado",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMessage ?: "Ocurrió un error al procesar el pago.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        checkoutViewModel.reset()
                        onError()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                ) {
                    Text("Volver", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (!isConfirmed) {
                // ── Estado: Procesando Pago ────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Burgundy.copy(alpha = 0.15f))
                        .border(2.dp, Burgundy.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Burgundy,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(60.dp),
                    )
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "Procesando Pago",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Simulando respuesta bancaria…\n(RF-08: exactamente 4 segundos)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                // ── Estado: Pagado ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scaleAnim)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(MetallicGold.copy(alpha = 0.2f), Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MetallicGold,
                        modifier = Modifier.size(80.dp),
                    )
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "¡Pago Confirmado!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tu pedido ha sido recibido",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                // Comprobante visual (RF-05 final)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainer, RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                ) {
                    Text("Comprobante de pago", style = MaterialTheme.typography.labelMedium, color = MetallicGold)
                    Spacer(Modifier.height(12.dp))
                    ReceiptRow("Subtotal", "$${String.format("%.2f", summary.subtotal)}")
                    ReceiptRow(
                        if (summary.distanceKm != null) "Envío (${"%.1f".format(summary.distanceKm)} km)" else "Envío",
                        "$${String.format("%.2f", summary.shippingFee ?: 25.0)}",
                    )
                    Divider(color = OutlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    ReceiptRow("Total cobrado", "$${String.format("%.2f", summary.total)}", highlight = true)
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        checkoutViewModel.reset()
                        onFinish()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver al inicio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) MaterialTheme.colorScheme.onBackground else OnSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) MetallicGold else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
