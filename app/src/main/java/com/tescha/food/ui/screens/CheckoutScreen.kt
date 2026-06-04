package com.tescha.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tescha.food.data.model.User
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.CartSummary
import com.tescha.food.ui.viewmodel.CheckoutForm
import com.tescha.food.ui.viewmodel.CheckoutViewModel
import com.tescha.food.ui.viewmodel.PaymentState

// RF-07: Módulo de simulación de pago con tarjeta
// RNF-07: Validación local con Luhn + fecha futura
@Composable
fun CheckoutScreen(
    summary: CartSummary,
    checkoutViewModel: CheckoutViewModel,
    currentUser: User?,
    onBack: () -> Unit,
    onPaymentProcessing: () -> Unit,
) {
    val form by checkoutViewModel.form.collectAsState()
    val paymentState by checkoutViewModel.paymentState.collectAsState()
    val errorMessage by checkoutViewModel.errorMessage.collectAsState()

    LaunchedEffect(paymentState) {
        if (paymentState == PaymentState.PROCESSING || paymentState == PaymentState.CONFIRMED) {
            onPaymentProcessing()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
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
            Column {
                Text(
                    "Pago seguro simulado",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "RF-07 · Datos ficticios de prueba",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Tarjeta visual ───────────────────────────────────────────────────
        CardVisual(form = form)

        Spacer(Modifier.height(24.dp))

        // ── Formulario ───────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            CheckoutField(
                label = "Nombre del titular",
                value = form.cardHolder,
                onValueChange = checkoutViewModel::onCardHolder,
                error = form.cardHolderError,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                placeholder = "Como aparece en la tarjeta",
            )

            Spacer(Modifier.height(14.dp))

            CheckoutField(
                label = "Número de tarjeta (16 dígitos)",
                value = form.cardNumber,
                onValueChange = checkoutViewModel::onCardNumber,
                error = form.cardNumberError,
                keyboardType = KeyboardType.Number,
                placeholder = "0000 0000 0000 0000",
                leadingIcon = {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = MetallicGold)
                },
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    CheckoutField(
                        label = "Fecha de expiración",
                        value = form.expiry,
                        onValueChange = checkoutViewModel::onExpiry,
                        error = form.expiryError,
                        keyboardType = KeyboardType.Number,
                        placeholder = "MM/AA",
                    )
                }
                Box(Modifier.weight(1f)) {
                    CheckoutField(
                        label = "CVV",
                        value = form.cvv,
                        onValueChange = checkoutViewModel::onCvv,
                        error = form.cvvError,
                        keyboardType = KeyboardType.Number,
                        placeholder = "123",
                        isPassword = true,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Outline)
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Resumen final antes de pagar (RF-05) ─────────────────────────
            MiniCostSummary(summary = summary)

            Spacer(Modifier.height(12.dp))

            // ── Saldo disponible del usuario ─────────────────────────────────
            if (currentUser != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Crédito disponible", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Text(
                        "$${String.format("%.2f", currentUser.balance)} MXN",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MetallicGold,
                    )
                }
            }

            // ── Mensaje de error (ej. saldo insuficiente) ────────────────────
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Botón pagar (RF-08: activa el simulador de 4s) ───────────────
            Button(
                onClick = { checkoutViewModel.pay(summary, currentUser) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                enabled = paymentState == PaymentState.IDLE,
            ) {
                Text(
                    "Pagar $${String.format("%.2f", summary.total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "* Simulación escolar — ningún cargo real se realizará",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CardVisual(form: CheckoutForm) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp)
            .background(
                Brush.linearGradient(listOf(Burgundy, Color(0xFF4A0E1D))),
                shape = RoundedCornerShape(16.dp),
            )
            .border(1.dp, MetallicGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TESCHA FOOD", style = MaterialTheme.typography.labelSmall, color = MetallicGold, letterSpacing = 2.sp)
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MetallicGold.copy(alpha = 0.7f))
            }
            Text(
                text = form.cardNumber.ifEmpty { "0000 0000 0000 0000" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 20.sp,
                ),
                color = Color.White,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TITULAR", style = MaterialTheme.typography.labelSmall, color = MetallicGold.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text(
                        form.cardHolder.uppercase().ifEmpty { "NOMBRE TITULAR" },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("EXPIRA", style = MaterialTheme.typography.labelSmall, color = MetallicGold.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text(
                        form.expiry.ifEmpty { "MM/AA" },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCostSummary(summary: CartSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainer, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text("Desglose del pago (RF-05)", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Text("$${String.format("%.2f", summary.subtotal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (summary.distanceKm != null) "Envío (${"%.1f".format(summary.distanceKm)} km)" else "Envío",
                style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant,
            )
            Text("$${String.format("%.2f", summary.shippingFee ?: 25.0)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Divider(color = OutlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            Text("$${String.format("%.2f", summary.total)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MetallicGold)
        }
    }
}

@Composable
private fun CheckoutField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    placeholder: String = "",
    isPassword: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = OutlineVariant) },
            leadingIcon = leadingIcon,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = MetallicGold,
                unfocusedBorderColor = OutlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            ),
            singleLine = true,
        )
    }
}
