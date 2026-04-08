package com.homenavigator.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<LatLng> = try {
        val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?: fusedClient.lastLocation.await()
        if (loc != null) Result.success(LatLng(loc.latitude, loc.longitude))
        else Result.failure(Exception("No se pudo obtener la ubicación"))
    } catch (e: Exception) { Result.failure(e) }

    @Suppress("DEPRECATION")
    suspend fun geocodeAddress(query: String): Result<Pair<LatLng, String>> = try {
        val results = Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1)
        val addr = results?.firstOrNull() ?: return Result.failure(Exception("Dirección no encontrada"))
        Result.success(Pair(LatLng(addr.latitude, addr.longitude), addr.getAddressLine(0) ?: query))
    } catch (e: Exception) { Result.failure(Exception("Error al geocodificar: ${e.message}")) }

    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(latLng: LatLng): String = try {
        Geocoder(context, Locale.getDefault())
            .getFromLocation(latLng.latitude, latLng.longitude, 1)
            ?.firstOrNull()?.getAddressLine(0)
            ?: "${latLng.latitude}, ${latLng.longitude}"
    } catch (e: Exception) { "${latLng.latitude}, ${latLng.longitude}" }
}