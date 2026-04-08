package com.homenavigator.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.homenavigator.data.model.*
import com.homenavigator.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val locationState by viewModel.locationState.collectAsState()
    val routeState    by viewModel.routeState.collectAsState()
    val homeAddress   by viewModel.homeAddress.collectAsState()
    val showSteps     by viewModel.showStepsPanel.collectAsState()
    val snackMsg      by viewModel.snackbarMessage.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()

    // MapView se crea una sola vez y se reusa — no se recrea en cada recomposición
    val mapView = rememberMapView()

    val perms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    LaunchedEffect(perms.allPermissionsGranted) {
        if (perms.allPermissionsGranted) viewModel.fetchCurrentLocation()
        else perms.launchMultiplePermissionRequest()
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHost.showSnackbar(it); viewModel.clearSnackbar() }
    }

    // Centrar mapa en ubicación actual — animación en hilo principal está bien,
    // es la API de osmdroid que es liviana
    LaunchedEffect(locationState) {
        (locationState as? LocationState.Available)?.let { state ->
            mapView.controller.animateTo(state.geoPoint, 14.0, 800)
        }
    }

    // Ajustar cámara para mostrar ruta completa
    LaunchedEffect(routeState) {
        (routeState as? RouteState.Success)?.let { s ->
            val points = s.routeInfo.polylinePoints
            if (points.size >= 2) {
                // Calcular bounding box en hilo de IO para no bloquear UI
                val box = withContext(Dispatchers.Default) {
                    BoundingBox.fromGeoPoints(points)
                }
                mapView.zoomToBoundingBox(box.increaseByScale(1.2f), true, 120)
            }
        }
    }

    // Actualizar overlays del mapa cuando cambia locationState, homeAddress o routeState
    // Se hace en DisposableEffect+derivedStateOf para evitar recomposiciones innecesarias
    val accentTealArgb      = AccentTeal.toArgb()
    val accentTealAlphaArgb = AccentTeal.copy(alpha = 0.4f).toArgb()

    LaunchedEffect(locationState, homeAddress, routeState) {
        // Mover el trabajo de overlays fuera del hilo principal
        withContext(Dispatchers.Main) {
            updateMapOverlays(
                mapView          = mapView,
                locationState    = locationState,
                homeAddress      = homeAddress,
                routeState       = routeState,
                accentTealArgb   = accentTealArgb,
                accentAlphaArgb  = accentTealAlphaArgb
            )
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = Color.Transparent) { _ ->
        Box(Modifier.fillMaxSize()) {

            // ── Mapa ──────────────────────────────────────────────────────
            AndroidView(
                factory  = { mapView },
                modifier = Modifier.fillMaxSize(),
                // update vacío — los overlays se manejan por LaunchedEffect
                update   = {}
            )

            // ── Barra superior ─────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(0.95f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (homeAddress.isConfigured) homeAddress.displayName
                            else "Sin dirección configurada",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                FilledTonalIconButton(onClick = onNavigateToSettings, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Settings, "Configuración")
                }
            }

            // ── FABs laterales ─────────────────────────────────────────────
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (perms.allPermissionsGranted) viewModel.fetchCurrentLocation()
                        else perms.launchMultiplePermissionRequest()
                    },
                    modifier = Modifier.size(48.dp), shape = CircleShape
                ) {
                    if (locationState is LocationState.Loading)
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else
                        Icon(
                            Icons.Default.MyLocation, "Mi ubicación",
                            tint = if (locationState is LocationState.Available) AccentTeal
                            else MaterialTheme.colorScheme.onSurface
                        )
                }
                if (routeState is RouteState.Success)
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleStepsPanel() },
                        modifier = Modifier.size(48.dp), shape = CircleShape
                    ) {
                        Icon(Icons.Default.List, "Instrucciones", tint = AccentAmber)
                    }
            }

            // ── Panel inferior ─────────────────────────────────────────────
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    routeState is RouteState.Success,
                    enter = slideInVertically { it }, exit = slideOutVertically { it }
                ) {
                    (routeState as? RouteState.Success)?.let {
                        RouteInfoCard(it.routeInfo) { viewModel.clearRoute() }
                    }
                }
                Button(
                    onClick = { viewModel.navigateHome() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    enabled = routeState !is RouteState.Loading
                ) {
                    if (routeState is RouteState.Loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = DeepNavy, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculando ruta…", color = DeepNavy, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Home, null, tint = DeepNavy, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (routeState is RouteState.Success) "Recalcular ruta a casa"
                            else "Navegar a casa",
                            color = DeepNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }
            }

            // ── Panel de pasos ─────────────────────────────────────────────
            AnimatedVisibility(
                showSteps && routeState is RouteState.Success,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it }, exit = slideOutVertically { it }
            ) {
                (routeState as? RouteState.Success)?.let {
                    StepsPanel(it.routeInfo) { viewModel.toggleStepsPanel() }
                }
            }

            // ── Pantalla de permisos ───────────────────────────────────────
            if (!perms.allPermissionsGranted) {
                Box(
                    Modifier.fillMaxSize().background(DeepNavy.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        Modifier.padding(32.dp), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = AccentTeal, modifier = Modifier.size(48.dp))
                            Text("Permiso de ubicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "HomeNavigator necesita tu ubicación para calcular la ruta hacia casa.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { perms.launchMultiplePermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                            ) {
                                Text("Conceder permiso", color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Crea y recuerda el MapView durante todo el ciclo de vida del composable.
// Al usar `remember` el MapView NO se recrea en cada recomposición.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(19.4326, -99.1332))
        }
    }
    // Gestionar onResume / onPause del MapView correctamente
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
    return mapView
}

// ─────────────────────────────────────────────────────────────────────────────
// Actualiza overlays del mapa. Se llama solo cuando cambia el estado,
// no en cada frame de Compose. Siempre en Dispatchers.Main.
// ─────────────────────────────────────────────────────────────────────────────
private fun updateMapOverlays(
    mapView: MapView,
    locationState: LocationState,
    homeAddress: com.homenavigator.data.model.HomeAddress,
    routeState: RouteState,
    accentTealArgb: Int,
    accentAlphaArgb: Int
) {
    mapView.overlays.clear()

    // Marcador ubicación actual
    (locationState as? LocationState.Available)?.let { state ->
        mapView.overlays.add(Marker(mapView).apply {
            position = state.geoPoint
            title    = "Mi ubicación"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
    }

    // Marcador casa
    if (homeAddress.isConfigured) {
        mapView.overlays.add(Marker(mapView).apply {
            position = homeAddress.geoPoint
            title    = "Casa"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
    }

    // Polylines de ruta
    (routeState as? RouteState.Success)?.let { s ->
        val points = s.routeInfo.polylinePoints
        // Sombra
        mapView.overlays.add(Polyline(mapView).apply {
            setPoints(points)
            outlinePaint.color       = accentAlphaArgb
            outlinePaint.strokeWidth = 20f
            outlinePaint.strokeCap   = Paint.Cap.ROUND
            outlinePaint.strokeJoin  = Paint.Join.ROUND
        })
        // Línea principal
        mapView.overlays.add(Polyline(mapView).apply {
            setPoints(points)
            outlinePaint.color       = accentTealArgb
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap   = Paint.Cap.ROUND
            outlinePaint.strokeJoin  = Paint.Join.ROUND
        })
    }

    mapView.invalidate()
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de información de ruta
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RouteInfoCard(routeInfo: RouteInfo, onClose: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.1f km".format(routeInfo.distanceKm),
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentTeal, fontWeight = FontWeight.ExtraBold
                )
                Text("distancia", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val m = routeInfo.durationMinutes.roundToInt()
                Text(
                    if (m >= 60) "${m/60}h ${m%60}min" else "${m}min",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentAmber, fontWeight = FontWeight.ExtraBold
                )
                Text("tiempo estimado", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Cerrar", modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de pasos de navegación
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepsPanel(routeInfo: RouteInfo, onDismiss: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Instrucciones (${routeInfo.steps.size} pasos)",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Cerrar", Modifier.size(18.dp))
                }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                items(routeInfo.steps) { step ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(AccentTeal))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(step.instruction, style = MaterialTheme.typography.bodyMedium)
                            if (step.distanceKm > 0.01)
                                Text(
                                    "%.0f m".format(step.distanceKm * 1000),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}
