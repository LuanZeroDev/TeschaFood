package com.tescha.food.data.repository.supabase

import com.tescha.food.data.model.User
import com.tescha.food.data.remote.dto.UserDto
import com.tescha.food.data.remote.supabase
import com.tescha.food.data.repository.UserRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseUserRepository : UserRepository {

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val authUser = supabase.auth.currentUserOrNull()
            ?: error("Login fallido: sin sesión activa")
        val uid = authUser.id
        val userEmail = authUser.email ?: email

        fetchUserById(uid) ?: run {
            // Usuario creado desde dashboard sin fila en users — crearla automáticamente
            supabase.from("users").insert(
                mapOf("id" to uid, "full_name" to userEmail.substringBefore("@"), "email" to userEmail, "role" to "cliente")
            )
            fetchUserById(uid) ?: error("No se pudo crear el perfil del usuario")
        }
    }

    override suspend fun register(name: String, email: String, phone: String, password: String): Result<User> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = kotlinx.serialization.json.buildJsonObject {
                put("full_name", kotlinx.serialization.json.JsonPrimitive(name))
            }
        }
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: error("Registro fallido: sin sesión activa")

        // Insertar fila en tabla users
        supabase.from("users").insert(
            mapOf(
                "id" to uid,
                "full_name" to name,
                "email" to email,
                "role" to "cliente",
            )
        )
        fetchUserById(uid) ?: error("Usuario no encontrado tras registro")
    }

    override suspend fun getCurrentUser(): User? {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return null
        return fetchUserById(uid)
    }

    override suspend fun getUserById(id: String): User? = fetchUserById(id)

    override suspend fun updateUser(user: User): User {
        supabase.from("users").update(
            mapOf("full_name" to user.name, "avatar_url" to user.avatarUrl)
        ) { filter { eq("id", user.id) } }
        return user
    }

    override suspend fun debitBalance(userId: String, amount: Double): Double {
        // RPC atómico: valida fondos y descuenta en una sola operación (ver migración SQL)
        return supabase.postgrest.rpc(
            "debit_user_balance",
            buildJsonObject {
                put("p_user_id", userId)
                put("p_amount", amount)
            }
        ).decodeAs()
    }

    override suspend fun logout() {
        supabase.auth.signOut()
    }

    private suspend fun fetchUserById(id: String): User? {
        val dto = supabase.from("users")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<UserDto>() ?: return null
        return dto.toDomain()
    }

    private suspend fun fetchUserByEmail(email: String): User? {
        val dto = supabase.from("users")
            .select { filter { eq("email", email) } }
            .decodeSingleOrNull<UserDto>() ?: return null
        return dto.toDomain()
    }

    private fun UserDto.toDomain(): User {
        // El bucket público `tescha-food` guarda las fotos como
        // `imagenes-usuarios/<prefijo-email>.jpg` (ej. miki@... → miki.jpg).
        // Si la columna avatar_url está vacía, derivamos la URL por convención.
        val resolved = avatarUrl?.takeIf { it.isNotBlank() }
            ?: defaultAvatarUrl(email)
        return User(
            id = id,
            name = fullName,
            email = email,
            phone = "",
            avatarUrl = resolved,
            isMerchant = role == "vendedor",
            balance = balance,
        )
    }

    private fun defaultAvatarUrl(email: String): String {
        val slug = email.substringBefore("@").lowercase()
        return "https://jpvppdqkaktkhvcfgfew.supabase.co/storage/v1/object/public/" +
            "tescha-food/imagenes-usuarios/$slug.jpg"
    }
}
