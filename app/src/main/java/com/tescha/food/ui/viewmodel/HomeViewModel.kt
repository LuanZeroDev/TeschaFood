package com.tescha.food.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.data.model.Product
import com.tescha.food.data.remote.DistanceMatrixService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.*

data class Category(val id: String, val label: String, val emoji: String)

data class ProductWithDistance(
    val product: Product,
    val distanceKm: Double?,
    val durationMinutes: Int? = null,  // RF-02: tiempo real vía Distance Matrix
)

// Categorías alineadas con los productos reales que existen en Supabase.
// La asignación se hace inferida por palabras clave en SupabaseProductRepository.
val ALL_CATEGORIES = listOf(
    Category("all",              "Todo",      "🍽️"),
    Category("cat-mexicana",     "Mexicana",  "🌶️"),
    Category("cat-tacos",        "Tacos",     "🌮"),
    Category("cat-pizza",        "Pizza",     "🍕"),
    Category("cat-hamburguesas", "Burgers",   "🍔"),
    Category("cat-parrilla",     "Parrilla",  "🥩"),
    Category("cat-postres",      "Postres",   "🍰"),
    Category("cat-antojitos",    "Antojitos", "🌽"),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val productRepository = (application as FoodApp).container.productRepository

    private val _allProducts     = MutableStateFlow<List<Product>>(emptyList())
    private val _searchQuery     = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("all")
    private val _userLocation    = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isLoading       = MutableStateFlow(true)

    val searchQuery:      StateFlow<String>                   = _searchQuery.asStateFlow()
    val selectedCategory: StateFlow<String>                   = _selectedCategory.asStateFlow()
    val isLoading:        StateFlow<Boolean>                  = _isLoading.asStateFlow()
    val userLocation:     StateFlow<Pair<Double, Double>?>    = _userLocation.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<ProductWithDistance>>(emptyList())
    val filteredProducts: StateFlow<List<ProductWithDistance>> = _filteredProducts.asStateFlow()

    init {
        loadProducts()
        viewModelScope.launch {
            combine(_allProducts, _searchQuery, _selectedCategory, _userLocation) {
                products, query, category, location ->
                applyFilters(products, query, category, location)
            }.collect { _filteredProducts.value = it }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _allProducts.value = productRepository.getProducts()
            _isLoading.value = false
        }
    }

    fun onSearchQuery(query: String)         { _searchQuery.value = query }
    fun onCategorySelected(categoryId: String) { _selectedCategory.value = categoryId }

    fun onUserLocation(lat: Double, lng: Double) {
        _userLocation.value = lat to lng
        // Cuando cambia la ubicación, lanzar actualización con Distance Matrix
        viewModelScope.launch { fetchRealDistances(lat, lng) }
    }

    // RF-02: enriquecer productos con distancias reales de Google Maps
    private suspend fun fetchRealDistances(userLat: Double, userLng: Double) {
        val products = _allProducts.value
        val enriched = products.map { product ->
            viewModelScope.async {
                val mLat = product.merchantLatitude ?: return@async product to null
                val mLng = product.merchantLongitude ?: return@async product to null
                val result = DistanceMatrixService.getDistance(userLat, userLng, mLat, mLng)
                product to result
            }
        }.awaitAll()

        // Actualizar productos con distancias reales (sin cambiar la lista base)
        val updated = enriched.map { (product, result) ->
            val km = result?.distanceKm ?: haversineKm(
                userLat, userLng,
                product.merchantLatitude ?: userLat,
                product.merchantLongitude ?: userLng,
            )
            ProductWithDistance(product, km, result?.durationMinutes)
        }
        _filteredProducts.value = updated
            .filter { applySearchAndCategory(it.product, _searchQuery.value, _selectedCategory.value) }
            .filter { it.distanceKm == null || it.distanceKm <= it.product.maxDeliveryKm } // RF-01
            .sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    private fun applyFilters(
        products: List<Product>,
        query: String,
        category: String,
        location: Pair<Double, Double>?,
    ): List<ProductWithDistance> {
        return products
            .filter { applySearchAndCategory(it, query, category) }
            .map { product ->
                val dist = location?.let {
                    val mLat = product.merchantLatitude ?: return@let null
                    val mLng = product.merchantLongitude ?: return@let null
                    haversineKm(it.first, it.second, mLat, mLng)
                }
                ProductWithDistance(product, dist)
            }
            // RF-01: excluir productos fuera del radio de entrega del vendedor
            .filter { it.distanceKm == null || it.distanceKm <= it.product.maxDeliveryKm }
            // RF-04: ordenar por cercanía
            .sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    private fun applySearchAndCategory(product: Product, query: String, category: String): Boolean {
        val matchQuery = query.isBlank() ||
            product.name.contains(query, ignoreCase = true) ||
            product.description.contains(query, ignoreCase = true) ||
            product.merchantName.contains(query, ignoreCase = true)
        val matchCat = category == "all" || product.categoryId == category
        return matchQuery && matchCat
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
