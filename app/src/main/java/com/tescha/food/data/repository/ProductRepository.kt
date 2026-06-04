package com.tescha.food.data.repository

import com.tescha.food.data.model.Product

interface ProductRepository {
    suspend fun getProducts(): List<Product>
    suspend fun getProductById(id: String): Product?
    suspend fun getProductsByMerchant(merchantId: String): List<Product>
    suspend fun getProductsByCategory(categoryId: String): List<Product>
    suspend fun searchProducts(query: String): List<Product>
}
