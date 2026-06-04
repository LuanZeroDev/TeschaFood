package com.tescha.food.data.repository

import com.tescha.food.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, phone: String, password: String): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun getUserById(id: String): User?
    suspend fun updateUser(user: User): User
    /** Descuenta [amount] del saldo del usuario de forma atómica. Devuelve el nuevo saldo. */
    suspend fun debitBalance(userId: String, amount: Double): Double
    suspend fun logout()
}
