package com.tescha.food.data.repository.supabase

import com.tescha.food.data.model.Product
import com.tescha.food.data.remote.dto.ProductDto
import com.tescha.food.data.remote.dto.StoreDto
import com.tescha.food.data.remote.supabase
import com.tescha.food.data.repository.ProductRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

// URL pública del bucket de imágenes
private const val STORAGE_URL =
    "https://jpvppdqkaktkhvcfgfew.supabase.co/storage/v1/object/public/tescha-food/"

class SupabaseProductRepository : ProductRepository {

    // RF-03: solo tiendas activas · RF-04: ordenadas por cercanía en HomeViewModel
    override suspend fun getProducts(): List<Product> {
        val stores = supabase.from("stores")
            .select { filter { eq("status", "activo") } }
            .decodeList<StoreDto>()

        val storeMap = stores.associateBy { it.id }

        val products = supabase.from("products")
            .select { filter { eq("available", true) } }
            .decodeList<ProductDto>()

        return products
            .filter { storeMap.containsKey(it.storeId) }
            .map { it.toDomain(storeMap[it.storeId]!!) }
    }

    override suspend fun getProductById(id: String): Product? {
        val dto = supabase.from("products")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<ProductDto>() ?: return null

        val store = supabase.from("stores")
            .select { filter { eq("id", dto.storeId) } }
            .decodeSingleOrNull<StoreDto>() ?: return null

        return dto.toDomain(store)
    }

    override suspend fun getProductsByMerchant(merchantId: String): List<Product> {
        val store = supabase.from("stores")
            .select { filter { eq("id", merchantId) } }
            .decodeSingleOrNull<StoreDto>() ?: return emptyList()

        return supabase.from("products")
            .select { filter { eq("store_id", merchantId) } }
            .decodeList<ProductDto>()
            .map { it.toDomain(store) }
    }

    override suspend fun getProductsByCategory(categoryId: String): List<Product> =
        getProducts().filter { it.categoryId == categoryId }

    override suspend fun searchProducts(query: String): List<Product> =
        getProducts().filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.merchantName.contains(query, ignoreCase = true)
        }

    private fun ProductDto.toDomain(store: StoreDto) = Product(
        id = id,
        name = name,
        description = description ?: "",
        price = price,
        imageUrl = imageUrl?.let { STORAGE_URL + it } ?: "",
        categoryId = inferCategory(name, description ?: "", store.name),
        merchantId = store.id,
        maxDeliveryKm = store.maxDeliveryKm,
        merchantName = store.name,
        merchantLatitude = store.latitude,
        merchantLongitude = store.longitude,
        isAvailable = available,
    )

    /**
     * Infiere la categoría del producto a partir de palabras clave en el nombre,
     * la descripción y el nombre de la tienda. Necesario porque la tabla
     * `products` no tiene columna `category_id`. El orden importa: las reglas
     * más específicas primero.
     */
    private fun inferCategory(name: String, description: String, storeName: String): String {
        val haystack = "$name $description $storeName".lowercase()
        return when {
            // Tacos y derivados
            "taco" in haystack || "quesadilla" in haystack || "tinga" in haystack -> "cat-tacos"
            // Pizza
            "pizza" in haystack -> "cat-pizza"
            // Hamburguesas
            "hamburgues" in haystack || "burger" in haystack -> "cat-hamburguesas"
            // Postres
            "pastel"  in haystack || "pay"   in haystack || "postre" in haystack ||
            "helado"  in haystack || "flan"  in haystack || "dulce"  in haystack ||
            "brownie" in haystack || "chocolate" in haystack -> "cat-postres"
            // Antojitos
            "elote"   in haystack || "esquite" in haystack || "chicharr" in haystack ||
            "tostada" in haystack || "sope"    in haystack || "gordita"  in haystack -> "cat-antojitos"
            // Parrilla / carnes
            "bistec"  in haystack || "broche"  in haystack || "asad"   in haystack ||
            "parrill" in haystack || "costill" in haystack || "arrach" in haystack ||
            "milanesa" in haystack -> "cat-parrilla"
            // Cocina mexicana tradicional
            "mole"    in haystack || "chile"   in haystack || "pozole" in haystack ||
            "tamal"   in haystack || "enchilad" in haystack || "nogada" in haystack ||
            "oaxaqu"  in haystack -> "cat-mexicana"
            // Default → mexicana (es lo más amplio y la mayoría del catálogo es mexicano)
            else -> "cat-mexicana"
        }
    }
}
