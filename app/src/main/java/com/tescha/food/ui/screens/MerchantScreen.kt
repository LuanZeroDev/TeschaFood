package com.tescha.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.MerchantViewModel

// RF-09: Configuración del PDV con mapa interactivo
// RF-15: Autonomía de reglas del local (horario, radio, estado)
@Composable
fun MerchantScreen(merchantViewModel: MerchantViewModel = viewModel()) {
    val state by merchantViewModel.state.collectAsState()

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            merchantViewModel.clearMessage()
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MetallicGold)
        }
        return
    }

    if (state.store == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = OutlineVariant, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("No tienes una tienda asignada", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("Solicita el rol de vendedor en tu perfil", color = OutlineVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Mi Tienda", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                Text(state.store!!.name, style = MaterialTheme.typography.bodyMedium, color = MetallicGold)
            }
            // RF-15: toggle de estado del local
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.isActive) "Abierto" else "Cerrado", style = MaterialTheme.typography.labelMedium,
                    color = if (state.isActive) Color(0xFF4CAF50) else Error)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { merchantViewModel.toggleStatus(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Burgundy, checkedThumbColor = MetallicGold),
                )
            }
        }

        // ── Mapa para fijar coordenadas (RF-09, RNF-09) ──────────────────────
        SectionCard(title = "Ubicación del local (RF-09)", subtitle = "Toca el mapa para actualizar tu punto de venta") {
            val initialPos = state.selectedLocation ?: LatLng(state.store!!.latitude, state.store!!.longitude)
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(initialPos, 15f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    properties = MapProperties(mapType = MapType.NORMAL),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false),
                    onMapClick = { latLng -> merchantViewModel.setLocation(latLng) },
                ) {
                    state.selectedLocation?.let { loc ->
                        Marker(state = MarkerState(position = loc), title = "Tu local")
                    }
                }
            }

            state.selectedLocation?.let { loc ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }

        // ── Horarios (RF-15) ─────────────────────────────────────────────────
        SectionCard(title = "Horario de atención (RF-15)", subtitle = "Define cuándo está disponible tu local") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(
                    label = "Apertura",
                    value = state.openTime,
                    onValueChange = merchantViewModel::setOpenTime,
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = "Cierre",
                    value = state.closeTime,
                    onValueChange = merchantViewModel::setCloseTime,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Radio de entrega (RF-15, RF-06) ──────────────────────────────────
        SectionCard(title = "Radio máximo de reparto (RF-15)", subtitle = "Distancia máxima de entrega desde tu local") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("0 km", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Text(
                    "${"%.1f".format(state.maxDeliveryKm)} km",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MetallicGold,
                )
                Text("5 km", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
            Slider(
                value = state.maxDeliveryKm.toFloat(),
                onValueChange = { merchantViewModel.setMaxDeliveryKm(it.toDouble()) },
                valueRange = 0.5f..5.0f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = MetallicGold,
                    activeTrackColor = Burgundy,
                    inactiveTrackColor = SurfaceContainerHigh,
                ),
            )
            // Muestra la tarifa correspondiente según RF-06
            val fee = when {
                state.maxDeliveryKm <= 2.0 -> "Tarifa fija: $25.00"
                else -> "Tarifa máx: $${"%.2f".format(25.0 + (state.maxDeliveryKm - 2.0) * 10.0)}"
            }
            Text(fee, style = MaterialTheme.typography.labelSmall, color = MetallicGold)
        }

        // ── Guardar ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Button(
                onClick = { merchantViewModel.saveChanges() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar cambios", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, style = MaterialTheme.typography.bodySmall, color = MetallicGold, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 5) onValueChange(it) },
            placeholder = { Text("HH:MM", color = OutlineVariant) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = MetallicGold,
                unfocusedBorderColor = OutlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}
