package com.tescha.food.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.data.remote.dto.UserDto
import com.tescha.food.data.remote.dto.VendorRequestDto
import com.tescha.food.data.remote.dto.VendorRequestWithUser
import com.tescha.food.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val requests: List<VendorRequestWithUser> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init { loadRequests() }

    // RNF-14: cargar hasta 50 solicitudes pendientes
    fun loadRequests() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val requests = supabase.from("vendor_requests")
                    .select { filter { eq("status", "pendiente") }; limit(50) }
                    .decodeList<VendorRequestDto>()

                // Enriquecer con datos del usuario en segunda consulta
                val userIds = requests.map { it.userId }.distinct()
                val users = if (userIds.isNotEmpty()) {
                    supabase.from("users")
                        .select { filter { isIn("id", userIds as List<Any>) } }
                        .decodeList<UserDto>()
                        .associateBy { it.id }
                } else emptyMap()

                val rows = requests.map { req ->
                    VendorRequestWithUser(
                        id        = req.id ?: "",
                        userId    = req.userId,
                        status    = req.status,
                        docUrl    = req.docUrl,
                        docFormat = req.docFormat,
                        docSizeKb = req.docSizeKb,
                        createdAt = req.createdAt,
                        users     = users[req.userId],
                    )
                }
                _state.value = _state.value.copy(requests = rows, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, message = "Error al cargar: ${e.message}")
            }
        }
    }

    // RF-14: aprobar solicitud + actualizar rol del usuario a 'vendedor'
    fun approve(requestId: String, userId: String) {
        viewModelScope.launch {
            try {
                supabase.from("vendor_requests").update(mapOf("status" to "aprobado")) {
                    filter { eq("id", requestId) }
                }
                supabase.from("users").update(mapOf("role" to "vendedor")) {
                    filter { eq("id", userId) }
                }
                _state.value = _state.value.copy(
                    requests = _state.value.requests.filter { it.id != requestId },
                    message  = "Solicitud aprobada",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Error: ${e.message}")
            }
        }
    }

    // RF-14: rechazar solicitud
    fun reject(requestId: String) {
        viewModelScope.launch {
            try {
                supabase.from("vendor_requests").update(mapOf("status" to "rechazado")) {
                    filter { eq("id", requestId) }
                }
                _state.value = _state.value.copy(
                    requests = _state.value.requests.filter { it.id != requestId },
                    message  = "Solicitud rechazada",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Error: ${e.message}")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
