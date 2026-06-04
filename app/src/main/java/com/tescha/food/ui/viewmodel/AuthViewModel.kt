package com.tescha.food.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = (application as FoodApp).container.userRepository

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    private val _loginSuccess = MutableStateFlow<User?>(null)
    val loginSuccess: StateFlow<User?> = _loginSuccess.asStateFlow()

    fun onNameChange(value: String) { _formState.value = _formState.value.copy(name = value, error = null) }
    fun onEmailChange(value: String) { _formState.value = _formState.value.copy(email = value, error = null) }
    fun onPhoneChange(value: String) { _formState.value = _formState.value.copy(phone = value, error = null) }
    fun onPasswordChange(value: String) { _formState.value = _formState.value.copy(password = value, error = null) }

    fun login() {
        val state = _formState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _formState.value = state.copy(error = "Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true, error = null)
            val result = userRepository.login(state.email, state.password)
            result.fold(
                onSuccess = { user -> _loginSuccess.value = user },
                onFailure = { e ->
                    _formState.value = _formState.value.copy(isLoading = false, error = friendlyError(e))
                }
            )
        }
    }

    fun register() {
        val state = _formState.value
        if (state.name.isBlank() || state.email.isBlank() || state.phone.isBlank() || state.password.isBlank()) {
            _formState.value = state.copy(error = "Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _formState.value = state.copy(isLoading = true, error = null)
            val result = userRepository.register(state.name, state.email, state.phone, state.password)
            result.fold(
                onSuccess = { user -> _loginSuccess.value = user },
                onFailure = { e ->
                    _formState.value = _formState.value.copy(isLoading = false, error = friendlyError(e))
                }
            )
        }
    }

    fun clearSuccess() { _loginSuccess.value = null }

    // Convierte excepciones técnicas de Supabase en mensajes amigables en español.
    private fun friendlyError(e: Throwable): String {
        val raw = (e.message ?: "").lowercase()
        return when {
            "invalid_credentials" in raw || "invalid login" in raw ->
                "Correo o contraseña incorrectos."
            "email" in raw && "registered" in raw ->
                "Este correo ya está registrado."
            "user already registered" in raw ->
                "Este correo ya está registrado."
            "weak password" in raw || "password" in raw && "short" in raw ->
                "La contraseña no cumple los requisitos."
            "network" in raw || "unable to resolve host" in raw || "timeout" in raw ->
                "Sin conexión. Verifica tu internet e inténtalo de nuevo."
            "rate limit" in raw || "too many" in raw ->
                "Demasiados intentos. Espera un momento."
            raw.isBlank() -> "Ocurrió un error. Inténtalo de nuevo."
            else -> "No pudimos completar la operación. Inténtalo de nuevo."
        }
    }
}
