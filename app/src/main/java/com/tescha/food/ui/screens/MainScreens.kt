package com.tescha.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.tescha.food.data.model.Product
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.ALL_CATEGORIES
import com.tescha.food.ui.viewmodel.CartViewModel
import com.tescha.food.ui.viewmodel.HomeViewModel
import com.tescha.food.ui.viewmodel.ProductWithDistance
import kotlinx.coroutines.launch
import kotlin.math.abs

// ─────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userLocation: Pair<Double, Double>? = null,
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel? = null,
    onProductAdded: (() -> Unit)? = null,
) {
    val searchQuery      by homeViewModel.searchQuery.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val products         by homeViewModel.filteredProducts.collectAsState()
    val isLoading        by homeViewModel.isLoading.collectAsState()
    val cartSummary      = cartViewModel?.summary?.collectAsState()?.value
    val cartCount        = cartSummary?.items?.sumOf { it.quantity } ?: 0

    var selectedProduct by remember { mutableStateOf<ProductWithDistance?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(userLocation) {
        userLocation?.let { homeViewModel.onUserLocation(it.first, it.second) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HomeHeader(
                userLocation = userLocation,
                cartCount = cartCount,
                onCartClick = onProductAdded, // navega al carrito
            )

            HomeSearchBar(
                query = searchQuery,
                onQueryChange = homeViewModel::onSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
            )

            CategoryChips(
                categories = ALL_CATEGORIES,
                selected = selectedCategory,
                onSelect = homeViewModel::onCategorySelected,
            )

            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MetallicGold, strokeWidth = 2.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("Cargando productos…", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "Sin productos disponibles"
                                   else "Sin resultados para \"$searchQuery\"",
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(products) { index, item ->
                            ProductCard(
                                item = item,
                                cardHeight = cardHeightForIndex(index),
                                onAdd = {
                                    selectedProduct = item
                                    scope.launch { sheetState.show() }
                                },
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom Sheet de detalle del producto ─────────────────────────────
        selectedProduct?.let { item ->
            if (sheetState.isVisible || sheetState.targetValue != SheetValue.Hidden) {
                ModalBottomSheet(
                    onDismissRequest = {
                        scope.launch { sheetState.hide() }
                        selectedProduct = null
                    },
                    sheetState = sheetState,
                    containerColor = SurfaceContainerLow,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 8.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(OutlineVariant)
                        )
                    },
                ) {
                    ProductDetailSheet(
                        item = item,
                        onAddToCart = { quantity ->
                            repeat(quantity) { cartViewModel?.addProduct(item.product) }
                            scope.launch { sheetState.hide() }
                            selectedProduct = null
                            onProductAdded?.invoke()
                        },
                        onDismiss = {
                            scope.launch { sheetState.hide() }
                            selectedProduct = null
                        },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PRODUCT DETAIL BOTTOM SHEET
// ─────────────────────────────────────────────

@Composable
private fun ProductDetailSheet(
    item: ProductWithDistance,
    onAddToCart: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val product = item.product
    var quantity by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // Imagen grande
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (product.imageUrl.isNotEmpty()) {
                SubcomposeAsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { CircularProgressIndicator(color = MetallicGold, strokeWidth = 2.dp, modifier = Modifier.size(32.dp)) },
                    error = { Text(categoryEmoji(product.categoryId), fontSize = 48.sp) },
                )
            } else {
                Text(categoryEmoji(product.categoryId), fontSize = 64.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Nombre y tienda
        Text(
            product.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (product.merchantName.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Store, contentDescription = null, tint = Outline, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(product.merchantName, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                item.distanceKm?.let { km ->
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(13.dp))
                    Text(
                        if (km < 1.0) "${(km * 1000).toInt()}m" else "${"%.1f".format(km)}km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MetallicGold,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (product.description.isNotEmpty()) {
            Text(product.description, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }

        // Precio
        Text(
            "$${String.format("%.2f", product.price)}",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MetallicGold,
        )

        Spacer(Modifier.height(20.dp))

        // Selector de cantidad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Cantidad", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = { if (quantity > 1) quantity-- },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceContainerHigh),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Text(
                    "$quantity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.widthIn(min = 32.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { quantity++ },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Burgundy),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Más", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Botón agregar al carrito
        Button(
            onClick = { onAddToCart(quantity) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Burgundy),
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                "Agregar al carrito · $${String.format("%.2f", product.price * quantity)}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────
// HEADER con badge del carrito
// ─────────────────────────────────────────────

@Composable
private fun HomeHeader(
    userLocation: Pair<Double, Double>?,
    cartCount: Int,
    onCartClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Descubrir",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = Snow,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (userLocation != null) MintSignal else Slate300),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (userLocation != null) "Ordenado por cercanía" else "Activar ubicación",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                    color = Slate200,
                )
            }
        }

        // Badge del carrito — pill estilo Raycast con borde sutil
        if (onCartClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(DeepCharcoal)
                    .border(1.dp, Snow.copy(alpha = 0.08f), RoundedCornerShape(11.dp))
                    .clickable { onCartClick() }
                    .padding(11.dp),
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Carrito",
                    tint = Snow,
                    modifier = Modifier.size(22.dp),
                )
                if (cartCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(EmberRed),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$cartCount", color = Snow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SEARCH BAR
// ─────────────────────────────────────────────

@Composable
private fun HomeSearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Buscar platillos, tiendas…",
                color = Slate300,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate300) },
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DeepCharcoal,
            unfocusedContainerColor = DeepCharcoal,
            focusedBorderColor = Snow.copy(alpha = 0.18f),
            unfocusedBorderColor = Snow.copy(alpha = 0.06f),
            focusedTextColor = Snow,
            unfocusedTextColor = Snow,
            cursorColor = EmberRed,
        ),
        singleLine = true,
    )
}

// ─────────────────────────────────────────────
// CATEGORY CHIPS
// ─────────────────────────────────────────────

@Composable
private fun CategoryChips(
    categories: List<com.tescha.food.ui.viewmodel.Category>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { cat ->
            val isSelected = cat.id == selected
            val chipShape = RoundedCornerShape(8.dp)
            Box(
                modifier = Modifier
                    .clip(chipShape)
                    .background(if (isSelected) Ash50 else DeepCharcoal)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else Snow.copy(alpha = 0.08f),
                        shape = chipShape,
                    )
                    .clickable { onSelect(cat.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${cat.emoji}  ${cat.label}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    ),
                    color = if (isSelected) VoidBlack else Slate200,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// PRODUCT CARD — Raycast Obsidian Style
// ─────────────────────────────────────────────

@Composable
fun ProductCard(item: ProductWithDistance, cardHeight: Dp, onAdd: (() -> Unit)? = null) {
    val product = item.product
    val cardShape = RoundedCornerShape(11.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(cardShape)
            .background(DeepCharcoal)
            .border(1.dp, Snow.copy(alpha = 0.06f), cardShape)
            .then(if (onAdd != null) Modifier.clickable { onAdd() } else Modifier)
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Imagen contenida — esquinas redondeadas, glow sutil al fondo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Graphite700),
                contentAlignment = Alignment.Center,
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(
                                color = Snow.copy(alpha = 0.6f),
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        error = { Text(categoryEmoji(product.categoryId), fontSize = 36.sp) },
                    )
                } else {
                    Text(categoryEmoji(product.categoryId), fontSize = 36.sp)
                }

                // Chip de distancia flotante en esquina sup. derecha
                item.distanceKm?.let { km ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(VoidBlack.copy(alpha = 0.7f))
                            .border(1.dp, Snow.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = EmberRed,
                            modifier = Modifier.size(9.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (km < 1.0) "${(km * 1000).toInt()}m" else "${"%.1f".format(km)}km",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                            ),
                            color = Snow,
                        )
                    }
                }
            }

            // Nombre del producto — Inter SemiBold con tracking ligeramente negativo
            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    lineHeight = 16.sp,
                ),
                color = Snow,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Tienda — texto terciario apagado
            if (product.merchantName.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        tint = Slate300,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        product.merchantName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.3.sp,
                        ),
                        color = Slate300,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Precio prominente + chip de categoría — Snow blanco
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$${String.format("%.0f", product.price)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = Snow,
                )
                if (onAdd != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ash50),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar",
                            tint = VoidBlack,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PANTALLAS STUB
// ─────────────────────────────────────────────

@Composable
fun WalletScreen(balance: Double = 0.0) {
    var showBalance by remember { mutableStateOf(true) }

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
                .padding(top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Billetera",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showBalance = !showBalance }) {
                Icon(
                    if (showBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Ocultar saldo",
                    tint = MetallicGold,
                )
            }
        }

        // ── Tarjeta de crédito estética ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Burgundy, Color(0xFF5A0A1A), Color(0xFF2A0A0A)),
                    )
                ),
        ) {
            // Círculos decorativos glassmorphism
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = (-40).dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .clip(CircleShape)
                    .background(MetallicGold.copy(alpha = 0.10f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Chip + logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(listOf(MetallicGold, Color(0xFFF5D060)))
                            ),
                    )
                    Text(
                        "TESCHA FOOD",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = MetallicGold,
                    )
                }

                // Número de tarjeta
                Text(
                    "•••• •••• •••• 4827",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    color = Color.White,
                )

                // Titular + fecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "TITULAR",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            "ESTUDIANTE TESCHA",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "VÁLIDA HASTA",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            "12/28",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
            }

            // Borde metálico superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(MetallicGold, Color(0xFFF5D060), MetallicGold))),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Saldo disponible ─────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Saldo disponible",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (showBalance) "$${String.format("%.2f", balance)} MXN" else "•••••••",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MetallicGold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Acciones rápidas ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WalletActionButton(
                icon = Icons.Default.Add,
                label = "Recargar",
                modifier = Modifier.weight(1f),
            )
            WalletActionButton(
                icon = Icons.Default.Send,
                label = "Transferir",
                modifier = Modifier.weight(1f),
            )
            WalletActionButton(
                icon = Icons.Default.CreditCard,
                label = "QR Pay",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Movimientos recientes ────────────────────────────────────────────
        Text(
            "Movimientos recientes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        val movimientos = listOf(
            Triple("Pago en Tacos El Profe", "- $85.00", false),
            Triple("Recarga de saldo", "+ $200.00", true),
            Triple("Pago en Comida China", "- $65.00", false),
            Triple("Recarga de saldo", "+ $150.00", true),
        )

        movimientos.forEach { (desc, monto, esIngreso) ->
            WalletMovimientoRow(descripcion = desc, monto = monto, esIngreso = esIngreso)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WalletActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = label, tint = MetallicGold, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun WalletMovimientoRow(descripcion: String, monto: String, esIngreso: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (esIngreso) MetallicGold.copy(alpha = 0.15f) else Burgundy.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (esIngreso) Icons.Default.Add else Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = if (esIngreso) MetallicGold else Burgundy,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            monto,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (esIngreso) MetallicGold else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

private fun cardHeightForIndex(index: Int): Dp {
    val heights = listOf(200.dp, 240.dp, 180.dp, 260.dp, 220.dp, 190.dp)
    return heights[abs(index) % heights.size]
}

private fun categoryEmoji(categoryId: String): String = when (categoryId) {
    "cat-tacos"        -> "🌮"
    "cat-pizza"        -> "🍕"
    "cat-hamburguesas" -> "🍔"
    "cat-parrilla"     -> "🥩"
    "cat-postres"      -> "🍰"
    "cat-antojitos"    -> "🌽"
    "cat-mexicana"     -> "🌶️"
    else               -> "🍽️"
}
