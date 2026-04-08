package com.homenavigator.data.repository

import com.google.android.gms.maps.model.LatLng
import com.homenavigator.BuildConfig
import com.homenavigator.data.model.NavigationStep
import com.homenavigator.data.model.OrsDirectionsRequest
import com.homenavigator.data.model.RouteInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val orsApiService: OrsApiService
) {
    suspend fun getRoute(origin: LatLng, destination: LatLng): Result<RouteInfo> {
        return try {
            val request = OrsDirectionsRequest(
                coordinates = listOf(
                    listOf(origin.longitude, origin.latitude),
                    listOf(destination.longitude, destination.latitude)
                )
            )
            val response = orsApiService.getDirections(
                apiKey = BuildConfig.ORS_API_KEY,
                profile = "driving-car",
                request = request
            )
            val route = response.routes?.firstOrNull()
                ?: return Result.failure(Exception("No se encontró ninguna ruta"))
            val summary = route.summary
                ?: return Result.failure(Exception("Resumen de ruta no disponible"))

            val points = if (!route.geometry.isNullOrBlank())
                PolylineDecoder.decode(route.geometry)
            else listOf(origin, destination)

            val steps = route.segments?.flatMap { seg ->
                seg.steps?.map { step ->
                    NavigationStep(
                        instruction = step.instruction,
                        distanceKm = step.distance / 1000.0,
                        durationMinutes = step.duration / 60.0,
                        type = step.type
                    )
                } ?: emptyList()
            } ?: emptyList()

            Result.success(
                RouteInfo(
                    distanceKm = summary.distance,
                    durationMinutes = summary.duration / 60.0,
                    steps = steps,
                    polylinePoints = points
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}