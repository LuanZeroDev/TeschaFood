package com.tescha.food.data.repository.stub

import com.tescha.food.data.model.User
import com.tescha.food.data.repository.UserRepository

class StubUserRepository : UserRepository {

    private val stubUsers = mutableListOf(
        User(
            id = "user-1",
            name = "Ángel López",
            email = "angel@tescha.com",
            phone = "+52 55 0000 0000",
            address = "Av. Insurgentes Sur 123, CDMX",
            isMerchant = false,
        ),
        User(
            id = "user-2",
            name = "María Vendedora",
            email = "maria@tescha.com",
            phone = "+52 55 1111 2222",
            address = "Calle Reforma 45, CDMX",
            isMerchant = true,
        ),
    )

    private var currentUser: User? = null

    override suspend fun login(email: String, password: String): Result<User> {
        val user = stubUsers.find { it.email.equals(email, ignoreCase = true) }
        return if (user != null) {
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(Exception("Credenciales incorrectas"))
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<User> {
        if (stubUsers.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(Exception("El correo ya está registrado"))
        }
        val newUser = User(
            id = "user-${System.currentTimeMillis()}",
            name = name,
            email = email,
            phone = phone,
        )
        stubUsers.add(newUser)
        currentUser = newUser
        return Result.success(newUser)
    }

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun getUserById(id: String): User? =
        stubUsers.find { it.id == id }

    override suspend fun updateUser(user: User): User {
        val index = stubUsers.indexOfFirst { it.id == user.id }
        if (index >= 0) stubUsers[index] = user
        if (currentUser?.id == user.id) currentUser = user
        return user
    }

    override suspend fun debitBalance(userId: String, amount: Double): Double {
        val index = stubUsers.indexOfFirst { it.id == userId }
        require(index >= 0) { "Usuario $userId no encontrado" }
        val current = stubUsers[index]
        require(current.balance >= amount) { "Saldo insuficiente" }
        val updated = current.copy(balance = current.balance - amount)
        stubUsers[index] = updated
        if (currentUser?.id == userId) currentUser = updated
        return updated.balance
    }

    override suspend fun logout() {
        currentUser = null
    }
}
