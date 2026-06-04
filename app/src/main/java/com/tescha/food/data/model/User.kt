package com.tescha.food.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String = "",
    val avatarUrl: String = "",
    val isMerchant: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val balance: Double = 0.0,   // saldo/crédito disponible para comprar
)
