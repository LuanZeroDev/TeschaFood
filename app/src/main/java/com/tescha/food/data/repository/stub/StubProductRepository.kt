package com.tescha.food.data.repository.stub

import com.tescha.food.data.model.Product
import com.tescha.food.data.repository.ProductRepository

class StubProductRepository : ProductRepository {

    private val products = listOf(
        Product(
            id = "p1", name = "Tacos de Canasta", description = "3 tacos surtidos de frijol, papa y chicharrón",
            price = 45.0, categoryId = "cat-tacos", merchantId = "m1", merchantName = "Taquería El Paisa",
            merchantLatitude = 19.4326, merchantLongitude = -99.1332,
        ),
        Product(
            id = "p2", name = "Torta Cubana", description = "Torta con milanesa, jamón, queso, aguacate y jalapeños",
            price = 75.0, categoryId = "cat-tortas", merchantId = "m1", merchantName = "Taquería El Paisa",
            merchantLatitude = 19.4326, merchantLongitude = -99.1332,
        ),
        Product(
            id = "p3", name = "Agua de Jamaica", description = "500ml natural sin azúcar añadida",
            price = 25.0, categoryId = "cat-bebidas", merchantId = "m1", merchantName = "Taquería El Paisa",
            merchantLatitude = 19.4326, merchantLongitude = -99.1332,
        ),
        Product(
            id = "p4", name = "Pizza Margarita", description = "Salsa de tomate, mozzarella fresca y albahaca",
            price = 120.0, categoryId = "cat-pizza", merchantId = "m2", merchantName = "Pizzería Bella Roma",
            merchantLatitude = 19.4284, merchantLongitude = -99.1277,
        ),
        Product(
            id = "p5", name = "Pizza Pepperoni", description = "Salsa de tomate, queso y pepperoni premium",
            price = 145.0, categoryId = "cat-pizza", merchantId = "m2", merchantName = "Pizzería Bella Roma",
            merchantLatitude = 19.4284, merchantLongitude = -99.1277,
        ),
        Product(
            id = "p6", name = "Paracetamol 500mg", description = "Caja con 20 tabletas. Analgésico y antipirético",
            price = 58.0, categoryId = "cat-farmacia", merchantId = "m3", merchantName = "Farmacia del Ahorro",
            merchantLatitude = 19.4350, merchantLongitude = -99.1408,
        ),
        Product(
            id = "p7", name = "Vitamina C 1000mg", description = "30 tabletas efervescentes sabor naranja",
            price = 89.0, categoryId = "cat-farmacia", merchantId = "m3", merchantName = "Farmacia del Ahorro",
            merchantLatitude = 19.4350, merchantLongitude = -99.1408,
        ),
        Product(
            id = "p8", name = "Hamburguesa Clásica", description = "Res 180g, lechuga, tomate, cebolla y queso americano",
            price = 95.0, categoryId = "cat-hamburguesas", merchantId = "m4", merchantName = "Burger Spot",
            merchantLatitude = 19.4310, merchantLongitude = -99.1360,
        ),
        Product(
            id = "p9", name = "Orden de Papas Fritas", description = "Papas crujientes con salsa a elegir",
            price = 45.0, categoryId = "cat-hamburguesas", merchantId = "m4", merchantName = "Burger Spot",
            merchantLatitude = 19.4310, merchantLongitude = -99.1360,
        ),
        Product(
            id = "p10", name = "Sushi Roll California", description = "8 piezas con cangrejo, pepino y aguacate",
            price = 165.0, categoryId = "cat-sushi", merchantId = "m5", merchantName = "Sakura Sushi",
            merchantLatitude = 19.4265, merchantLongitude = -99.1445,
        ),
        Product(
            id = "p11", name = "Ramen Tonkotsu", description = "Caldo de cerdo 12h, noodles, chashu y huevo marinado",
            price = 145.0, categoryId = "cat-sushi", merchantId = "m5", merchantName = "Sakura Sushi",
            merchantLatitude = 19.4265, merchantLongitude = -99.1445,
        ),
        Product(
            id = "p12", name = "Smoothie Verde", description = "Espinaca, manzana verde, jengibre y pepino",
            price = 65.0, categoryId = "cat-bebidas", merchantId = "m6", merchantName = "Verde Vida",
            merchantLatitude = 19.4295, merchantLongitude = -99.1380,
        ),
    )

    override suspend fun getProducts(): List<Product> = products

    override suspend fun getProductById(id: String): Product? =
        products.find { it.id == id }

    override suspend fun getProductsByMerchant(merchantId: String): List<Product> =
        products.filter { it.merchantId == merchantId }

    override suspend fun getProductsByCategory(categoryId: String): List<Product> =
        products.filter { it.categoryId == categoryId }

    override suspend fun searchProducts(query: String): List<Product> =
        products.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.merchantName.contains(query, ignoreCase = true)
        }
}
