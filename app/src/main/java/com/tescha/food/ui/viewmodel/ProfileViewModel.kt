package com.tescha.food.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.data.model.User
import com.tescha.food.data.remote.dto.VendorRequestDto
import com.tescha.food.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VendorRequestStatus { NONE, PENDIENTE, APROBADO, RECHAZADO }

data class ProfileUiState(
    val user: User? = null,
    val vendorStatus: VendorRequestStatus = VendorRequestStatus.NONE,
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val message: String? = null,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val container      = (application as FoodApp).container
    private val userRepository = container.userRepository
    private val sessionManager = container.sessionManager

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            val vendorStatus = user?.let { fetchVendorStatus(it.id) } ?: VendorRequestStatus.NONE
            _state.value = ProfileUiState(user = user, vendorStatus = vendorStatus, isLoading = false)
        }
    }

    private suspend fun fetchVendorStatus(userId: String): VendorRequestStatus {
        return try {
            val rows = supabase.from("vendor_requests")
                .select { filter { eq("user_id", userId) } }
                .decodeList<VendorRequestDto>()
            when (rows.lastOrNull()?.status) {
                "aprobado"  -> VendorRequestStatus.APROBADO
                "rechazado" -> VendorRequestStatus.RECHAZADO
                "pendiente" -> VendorRequestStatus.PENDIENTE
                else        -> VendorRequestStatus.NONE
            }
        } catch (_: Exception) { VendorRequestStatus.NONE }
    }

    // RF-13: enviar solicitud de registro de vendedor
    fun submitVendorRequest(docName: String, docSizeKb: Int) {
        val userId = sessionManager.userId ?: return
        val format = when {
            docName.endsWith(".pdf",  ignoreCase = true) -> "pdf"
            docName.endsWith(".jpeg", ignoreCase = true) -> "jpeg"
            docName.endsWith(".png",  ignoreCase = true) -> "png"
            else -> "pdf"
        }
        // RNF-13: validar tamaño máx 5 MB
        if (docSizeKb > 5120) {
            _state.value = _state.value.copy(message = "El documento excede 5 MB (RNF-13)")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)
            try {
                supabase.from("vendor_requests").insert(
                    VendorRequestDto(
                        userId    = userId,
                        docUrl    = "docs/$docName",
                        docFormat = format,
                        docSizeKb = docSizeKb,
                    )
                )
                _state.value = _state.value.copy(
                    isSending    = false,
                    vendorStatus = VendorRequestStatus.PENDIENTE,
                    message      = "Solicitud enviada. El administrador la revisará pronto.",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSending = false, message = "Error: ${e.message}")
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            userRepository.logout()
            sessionManager.clearSession()
            onDone()
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
