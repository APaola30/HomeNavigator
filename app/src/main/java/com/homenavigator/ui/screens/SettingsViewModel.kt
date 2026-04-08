package com.homenavigator.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homenavigator.data.model.HomeAddress
import com.homenavigator.data.repository.HomeAddressRepository
import com.homenavigator.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val homeRepo: HomeAddressRepository,
    private val locationRepo: LocationRepository
) : ViewModel() {

    val homeAddress = homeRepo.homeAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeAddress())

    private val _loading = MutableStateFlow(false)
    val isLoading = _loading.asStateFlow()

    private val _geoResult = MutableStateFlow<GeoResult>(GeoResult.Idle)
    val geocodeResult = _geoResult.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    sealed class GeoResult {
        data object Idle : GeoResult()
        data class Found(val geoPoint: GeoPoint, val displayName: String) : GeoResult()
        data class Error(val msg: String) : GeoResult()
    }

    fun searchAddress(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _loading.value = true; _geoResult.value = GeoResult.Idle
            locationRepo.geocodeAddress(query)
                .onSuccess { (gp, name) -> _geoResult.value = GeoResult.Found(gp, name) }
                .onFailure { _geoResult.value = GeoResult.Error(it.message ?: "No encontrado") }
            _loading.value = false
        }
    }

    fun saveHomeFromMap(geoPoint: GeoPoint) {
        viewModelScope.launch {
            _loading.value = true
            val name = locationRepo.reverseGeocode(geoPoint)
            homeRepo.saveHomeAddress(HomeAddress(name, geoPoint.latitude, geoPoint.longitude, true))
            _message.value = "¡Casa guardada!"
            _loading.value = false
        }
    }

    fun confirmSaveGeocodeResult() {
        val r = _geoResult.value as? GeoResult.Found ?: return
        viewModelScope.launch {
            homeRepo.saveHomeAddress(HomeAddress(r.displayName, r.geoPoint.latitude, r.geoPoint.longitude, true))
            _message.value = "¡Casa guardada!"; _geoResult.value = GeoResult.Idle
        }
    }

    fun clearHome() {
        viewModelScope.launch { homeRepo.clearHomeAddress(); _message.value = "Dirección eliminada" }
    }

    fun clearMessage() { _message.value = null }
}
