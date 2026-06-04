package com.tescha.food.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.data.remote.dto.StoreDto
import com.tescha.food.data.remote.supabase
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MerchantUiState(
    val store: StoreDto? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
    // Campos editables (RF-15)
    val isActive: Boolean = true,
    val openTime: String = "08:00",
    val closeTime: String = "20:00",
    val maxDeliveryKm: Double = 5.0,
    val selectedLocation: LatLng? = null,   // RF-09
)

class MerchantViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = (application as FoodApp).container.sessionManager

    private val _state = MutableStateFlow(MerchantUiState())
    val state: StateFlow<MerchantUiState> = _state.asStateFlow()

    init { loadStore() }

    private fun loadStore() {
        val ownerId = sessionManager.userId ?: return
        viewModelScope.launch {
            try {
                val store = supabase.from("stores")
                    .select { filter { eq("owner_id", ownerId) } }
                    .decodeSingleOrNull<StoreDto>()

                _state.value = _state.value.copy(
                    store          = store,
                    isLoading      = false,
                    isActive       = store?.status == "activo",
                    openTime       = store?.openTime ?: "08:00",
                    closeTime      = store?.closeTime ?: "20:00",
                    maxDeliveryKm  = store?.maxDeliveryKm ?: 5.0,
                    selectedLocation = store?.let { LatLng(it.latitude, it.longitude) },
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    // RF-15: togglear estado del local
    fun toggleStatus(active: Boolean) {
        _state.value = _state.value.copy(isActive = active)
    }

    // RF-09: usuario fija coordenadas tocando el mapa
    fun setLocation(latLng: LatLng) {
        _state.value = _state.value.copy(selectedLocation = latLng)
    }

    fun setOpenTime(time: String)      { _state.value = _state.value.copy(openTime = time) }
    fun setCloseTime(time: String)     { _state.value = _state.value.copy(closeTime = time) }
    fun setMaxDeliveryKm(km: Double)   { _state.value = _state.value.copy(maxDeliveryKm = km) }

    // RF-09 + RF-15: guardar todos los parámetros en Supabase
    fun saveChanges() {
        val storeId  = _state.value.store?.id ?: return
        val location = _state.value.selectedLocation
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val update = buildMap<String, Any?> {
                    put("status",          if (_state.value.isActive) "activo" else "cerrado")
                    put("open_time",       _state.value.openTime)
                    put("close_time",      _state.value.closeTime)
                    put("max_delivery_km", _state.value.maxDeliveryKm)
                    location?.let {
                        put("latitude",  it.latitude)
                        put("longitude", it.longitude)
                    }
                }
                supabase.from("stores").update(update) { filter { eq("id", storeId) } }
                _state.value = _state.value.copy(isSaving = false, message = "Cambios guardados")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, message = "Error: ${e.message}")
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}
