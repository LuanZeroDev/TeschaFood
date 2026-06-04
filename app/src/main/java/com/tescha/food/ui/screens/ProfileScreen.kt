package com.tescha.food.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.ProfileViewModel
import com.tescha.food.ui.viewmodel.VendorRequestStatus

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit = {},
) {
    val state by profileViewModel.state.collectAsState()
    val context = LocalContext.current

    // RF-13: selector de documento (PDF, JPEG, PNG) — RNF-13
    var selectedDocName by remember { mutableStateOf<String?>(null) }
    var selectedDocSizeKb by remember { mutableIntStateOf(0) }
    var showVendorDialog by remember { mutableStateOf(false) }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    selectedDocName = c.getString(nameIdx)
                    selectedDocSizeKb = (c.getLong(sizeIdx) / 1024).toInt()
                }
            }
        }
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            profileViewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val rawAvatar = state.user?.avatarUrl
                val email = state.user?.email
                // Si la columna avatar_url está vacía o no es una URL completa, construimos
                // la URL del bucket público por convención (<prefijo-email>.jpg).
                val avatarUrl = when {
                    !rawAvatar.isNullOrBlank() && rawAvatar.startsWith("http") -> rawAvatar
                    !email.isNullOrBlank() -> buildString {
                        append("https://jpvppdqkaktkhvcfgfew.supabase.co/storage/v1/object/public/")
                        append("tescha-food/imagenes-usuarios/")
                        append(email.substringBefore("@").lowercase())
                        append(".jpg")
                    }
                    else -> null
                }
                LaunchedEffect(avatarUrl) {
                    android.util.Log.d("ProfileScreen", "Cargando avatar: $avatarUrl")
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Burgundy)
                        .border(2.dp, MetallicGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarUrl != null) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                CircularProgressIndicator(color = MetallicGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            },
                            error = { errState ->
                                android.util.Log.e(
                                    "ProfileScreen",
                                    "Avatar falló a cargar. URL=$avatarUrl",
                                    errState.result.throwable,
                                )
                                Text(
                                    state.user?.name?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            },
                        )
                    } else {
                        Text(
                            state.user?.name?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    state.user?.name ?: "Cargando…",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    state.user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                RoleBadge(isMerchant = state.user?.isMerchant == true, vendorStatus = state.vendorStatus)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Sección de información ───────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            ProfileInfoCard(Icons.Default.Email, "Correo", state.user?.email ?: "—")

            // ── RF-13: solicitar rol de vendedor ─────────────────────────────
            if (!state.isLoading && state.user?.isMerchant == false) {
                when (state.vendorStatus) {
                    VendorRequestStatus.NONE -> {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("¿Quieres vender en Tescha Food?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Envía tu solicitud con un documento de identidad (PDF, JPEG o PNG, máx. 5 MB).", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                if (selectedDocName != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(selectedDocName!!, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { docPicker.launch("*/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicGold),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
                                    ) {
                                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (selectedDocName == null) "Subir doc." else "Cambiar", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val name = selectedDocName ?: return@Button
                                            profileViewModel.submitVendorRequest(name, selectedDocSizeKb)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
                                        enabled = selectedDocName != null && !state.isSending,
                                    ) {
                                        if (state.isSending) {
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Enviar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    VendorRequestStatus.PENDIENTE -> StatusCard("Solicitud enviada", "El administrador revisará tu solicitud pronto.", MetallicGold, Icons.Default.HourglassTop)
                    VendorRequestStatus.APROBADO  -> StatusCard("¡Solicitud aprobada!", "Ya puedes gestionar tu tienda.", Color(0xFF4CAF50), Icons.Default.CheckCircle)
                    VendorRequestStatus.RECHAZADO -> StatusCard("Solicitud rechazada", "Contáctate con el administrador.", Error, Icons.Default.Cancel)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Cerrar sesión ────────────────────────────────────────────────
            Button(
                onClick = { profileViewModel.logout(onLogout) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Error)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = Error, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Snackbar de mensaje ──────────────────────────────────────────────
        state.message?.let { msg ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                ) {
                    Text(msg, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = MetallicGold)
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(isMerchant: Boolean, vendorStatus: VendorRequestStatus) {
    val (label, color) = when {
        isMerchant -> "Vendedor" to MetallicGold
        vendorStatus == VendorRequestStatus.PENDIENTE -> "Solicitud pendiente" to MetallicGold.copy(alpha = 0.7f)
        else -> "Cliente" to OnSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ProfileInfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, subtitle: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}
