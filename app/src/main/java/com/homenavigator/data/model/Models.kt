package com.homenavigator.data.model

import org.osmdroid.util.GeoPoint

data class HomeAddress(
    val displayName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isConfigured: Boolean = false
) {
    val geoPoint: GeoPoint get() = GeoPoint(latitude, longitude)
}

data class OrsDirectionsRequest(
    val coordinates: List<List<Double>>,
    val instructions: Boolean = true,
    val language: String = "es",
    val units: String = "km"
)

data class OrsDirectionsResponse(
    val routes: List<OrsRoute>?,
    val metadata: OrsMetadata?
)

data class OrsRoute(
    val summary: OrsSummary?,
    val segments: List<OrsSegment>?,
    val geometry: String?,
    val bbox: List<Double>?
)

data class OrsSummary(
    val distance: Double,
    val duration: Double
)

data class OrsSegment(
    val distance: Double,
    val duration: Double,
    val steps: List<OrsStep>?
)

data class OrsStep(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val name: String,
    val type: Int,
    val way_points: List<Int>
)

data class OrsMetadata(
    val attribution: String?,
    val service: String?,
    val timestamp: Long?,
    val query: OrsQuery?
)

data class OrsQuery(
    val coordinates: List<List<Double>>?,
    val profile: String?,
    val format: String?
)

data class RouteInfo(
    val distanceKm: Double,
    val durationMinutes: Double,
    val steps: List<NavigationStep>,
    val polylinePoints: List<GeoPoint>          // ← GeoPoint de osmdroid
)

data class NavigationStep(
    val instruction: String,
    val distanceKm: Double,
    val durationMinutes: Double,
    val type: Int = 0
)

sealed class RouteState {
    data object Idle : RouteState()
    data object Loading : RouteState()
    data class Success(val routeInfo: RouteInfo) : RouteState()
    data class Error(val message: String) : RouteState()
}

sealed class LocationState {
    data object Idle : LocationState()
    data object Loading : LocationState()
    data class Available(val geoPoint: GeoPoint) : LocationState() // ← GeoPoint
    data class Error(val message: String) : LocationState()
}
