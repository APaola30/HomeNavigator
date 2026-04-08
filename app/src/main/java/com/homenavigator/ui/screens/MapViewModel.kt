package com.homenavigator.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homenavigator.data.model.*
import com.homenavigator.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepo: LocationRepository,
    private val routeRepo: RouteRepository,
    private val homeRepo: HomeAddressRepository
) : ViewModel() {

    val homeAddress = homeRepo.homeAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeAddress())

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState = _locationState.asStateFlow()

    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
    val routeState = _routeState.asStateFlow()

    private val _showSteps = MutableStateFlow(false)
    val showStepsPanel = _showSteps.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbar.asStateFlow()

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = LocationState.Loading
            locationRepo.getCurrentLocation()
                .onSuccess { _locationState.value = LocationState.Available(it) }
                .onFailure { _locationState.value = LocationState.Error(it.message ?: "Error"); _snackbar.value = it.message }
        }
    }

    fun navigateHome() {
        val home = homeAddress.value
        if (!home.isConfigured) { _snackbar.value = "Primero configura tu dirección de casa"; return }
        val loc = (_locationState.value as? LocationState.Available)?.geoPoint
            ?: run { _snackbar.value = "Obteniendo tu ubicación…"; fetchCurrentLocation(); return }
        viewModelScope.launch {
            _routeState.value = RouteState.Loading
            routeRepo.getRoute(loc, home.geoPoint)
                .onSuccess { _routeState.value = RouteState.Success(it) }
                .onFailure { _routeState.value = RouteState.Error(it.message ?: "Error"); _snackbar.value = it.message }
        }
    }

    fun clearRoute()       { _routeState.value = RouteState.Idle; _showSteps.value = false }
    fun toggleStepsPanel() { _showSteps.value = !_showSteps.value }
    fun clearSnackbar()    { _snackbar.value = null }
}
