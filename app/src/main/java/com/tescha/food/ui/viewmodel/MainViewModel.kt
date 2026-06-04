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

enum class AuthRoute { WELCOME, LOGIN, REGISTER }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as FoodApp).container
    private val sessionManager = container.sessionManager
    private val userRepository = container.userRepository
    private val locationService = container.locationService

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authRoute = MutableStateFlow(AuthRoute.WELCOME)
    val authRoute: StateFlow<AuthRoute> = _authRoute.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(
        sessionManager.locationLatitude?.let { lat ->
            sessionManager.locationLongitude?.let { lng -> lat to lng }
        }
    )
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    init {
        if (sessionManager.isLoggedIn) {
            viewModelScope.launch {
                _currentUser.value = userRepository.getCurrentUser()
            }
        }
    }

    fun navigateTo(route: AuthRoute) {
        _authRoute.value = route
    }

    /** Recarga el perfil actual desde la BD (ej. para reflejar el saldo tras una compra). */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            val refreshed = userRepository.getCurrentUser() ?: return@launch
            sessionManager.saveUser(refreshed)
            _currentUser.value = refreshed
        }
    }

    fun onLoginSuccess(user: User) {
        sessionManager.saveUser(user)
        _currentUser.value = user
        _isLoggedIn.value = true
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            sessionManager.clearSession()
            _currentUser.value = null
            _isLoggedIn.value = false
            _authRoute.value = AuthRoute.WELCOME
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        sessionManager.locationLatitude = lat
        sessionManager.locationLongitude = lng
        _userLocation.value = lat to lng
    }

    fun requestLocation() {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null) updateLocation(location.first, location.second)
        }
    }
}
