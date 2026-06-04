package com.tescha.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tescha.food.data.remote.dto.VendorRequestWithUser
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.AdminViewModel

// RF-14: Panel de validación de cuentas de vendedores
// RNF-14: hasta 50 solicitudes simultáneas sin superar 2s de carga
@Composable
fun AdminScreen(adminViewModel: AdminViewModel = viewModel()) {
    val state by adminViewModel.state.collectAsState()

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            adminViewModel.clearMessage()
        }
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
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Panel Admin",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text("RF-14 · Solicitudes de vendedores", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
            }
            IconButton(onClick = { adminViewModel.loadRequests() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = MetallicGold)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MetallicGold)
            }
            return@Column
        }

        state.message?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
            ) {
                Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MetallicGold)
            }
        }

        if (state.requests.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Sin solicitudes pendientes", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Text(
                "${state.requests.size} solicitud(es) pendiente(s)",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.requests) { req ->
                    VendorRequestCard(
                        request = req,
                        onApprove = { adminViewModel.approve(req.id, req.userId) },
                        onReject  = { adminViewModel.reject(req.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VendorRequestCard(
    request: VendorRequestWithUser,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Burgundy.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Burgundy, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        request.users?.fullName ?: "Usuario ${request.userId.takeLast(6)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        request.users?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = OutlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DocChip(request.docFormat.uppercase())
                DocChip("${request.docSizeKb} KB")
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Documento: ${request.docUrl}",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))

            // Botones aprobar / rechazar (RF-14)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Error),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rechazar")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aprobar", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DocChip(label: String) {
    Box(
        modifier = Modifier
            .background(SurfaceContainerHigh, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MetallicGold)
    }
}
