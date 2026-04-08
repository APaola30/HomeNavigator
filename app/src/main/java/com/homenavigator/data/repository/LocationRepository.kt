package com.homenavigator.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<GeoPoint> = try {
        val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?: fusedClient.lastLocation.await()
        if (loc != null) Result.success(GeoPoint(loc.latitude, loc.longitude))
        else Result.failure(Exception("No se pudo obtener la ubicación"))
    } catch (e: Exception) { Result.failure(e) }

    @Suppress("DEPRECATION")
    suspend fun geocodeAddress(query: String): Result<Pair<GeoPoint, String>> = try {
        val results = Geocoder(context, Locale.getDefault()).getFromLocationName(query, 1)
        val addr = results?.firstOrNull() ?: return Result.failure(Exception("Dirección no encontrada"))
        Result.success(Pair(GeoPoint(addr.latitude, addr.longitude), addr.getAddressLine(0) ?: query))
    } catch (e: Exception) { Result.failure(Exception("Error al geocodificar: ${e.message}")) }

    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(geoPoint: GeoPoint): String = try {
        Geocoder(context, Locale.getDefault())
            .getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
            ?.firstOrNull()?.getAddressLine(0)
            ?: "${geoPoint.latitude}, ${geoPoint.longitude}"
    } catch (e: Exception) { "${geoPoint.latitude}, ${geoPoint.longitude}" }
}
