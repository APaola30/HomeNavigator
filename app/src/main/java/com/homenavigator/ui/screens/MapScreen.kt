package com.homenavigator.ui.screens

import android.Manifest
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.homenavigator.data.model.*
import com.homenavigator.ui.theme.*
import kotlinx.coroutines.launch
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

    val snackbarHost  = remember { SnackbarHostState() }
    val cameraPos     = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.4326, -99.1332), 12f)
    }
    val scope = rememberCoroutineScope()

    val perms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    LaunchedEffect(perms.allPermissionsGranted) {
        if (perms.allPermissionsGranted) viewModel.fetchCurrentLocation()
        else perms.launchMultiplePermissionRequest()
    }
    LaunchedEffect(snackMsg) { snackMsg?.let { snackbarHost.showSnackbar(it); viewModel.clearSnackbar() } }
    LaunchedEffect(locationState) {
        (locationState as? LocationState.Available)?.let {
            scope.launch { cameraPos.animate(CameraUpdateFactory.newLatLngZoom(it.latLng, 14f), 800) }
        }
    }
    LaunchedEffect(routeState) {
        (routeState as? RouteState.Success)?.let { s ->
            if (s.routeInfo.polylinePoints.size >= 2) {
                val bounds = LatLngBounds.builder().apply { s.routeInfo.polylinePoints.forEach { include(it) } }.build()
                scope.launch { cameraPos.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120), 1000) }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = Color.Transparent) { _ ->
        Box(Modifier.fillMaxSize()) {

            // ── Mapa ──────────────────────────────────────────────────────
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPos,
                properties = MapProperties(
                    isMyLocationEnabled = perms.allPermissionsGranted,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
            ) {
                (locationState as? LocationState.Available)?.let {
                    Marker(MarkerState(it.latLng), title = "Mi ubicación",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                }
                if (homeAddress.isConfigured)
                    Marker(MarkerState(homeAddress.latLng), title = "Casa",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                (routeState as? RouteState.Success)?.let { s ->
                    Polyline(s.routeInfo.polylinePoints, color = DeepNavy.copy(alpha = 0.4f), width = 20f, zIndex = -1f)
                    Polyline(s.routeInfo.polylinePoints, color = AccentTeal, width = 14f, geodesic = true)
                }
            }

            // ── Barra superior ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(0.95f), tonalElevation = 4.dp) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (homeAddress.isConfigured) homeAddress.displayName else "Sin dirección configurada",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                FilledTonalIconButton(onClick = onNavigateToSettings, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Settings, "Configuración")
                }
            }

            // ── FABs laterales ────────────────────────────────────────────
            Column(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(
                    onClick = { if (perms.allPermissionsGranted) viewModel.fetchCurrentLocation() else perms.launchMultiplePermissionRequest() },
                    modifier = Modifier.size(48.dp), shape = CircleShape
                ) {
                    if (locationState is LocationState.Loading)
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else
                        Icon(Icons.Default.MyLocation, "Mi ubicación",
                            tint = if (locationState is LocationState.Available) AccentTeal else MaterialTheme.colorScheme.onSurface)
                }
                if (routeState is RouteState.Success)
                    FilledTonalIconButton(onClick = { viewModel.toggleStepsPanel() }, modifier = Modifier.size(48.dp), shape = CircleShape) {
                        Icon(Icons.Default.List, "Instrucciones", tint = AccentAmber)
                    }
            }

            // ── Panel inferior ────────────────────────────────────────────
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(routeState is RouteState.Success, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    (routeState as? RouteState.Success)?.let { RouteInfoCard(it.routeInfo) { viewModel.clearRoute() } }
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
                            if (routeState is RouteState.Success) "Recalcular ruta a casa" else "Navegar a casa",
                            color = DeepNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }
            }

            // ── Panel de pasos ────────────────────────────────────────────
            AnimatedVisibility(
                showSteps && routeState is RouteState.Success,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it }, exit = slideOutVertically { it }
            ) {
                (routeState as? RouteState.Success)?.let {
                    StepsPanel(it.routeInfo) { viewModel.toggleStepsPanel() }
                }
            }

            // ── Pantalla de permisos ──────────────────────────────────────
            if (!perms.allPermissionsGranted) {
                Box(Modifier.fillMaxSize().background(DeepNavy.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                    Card(Modifier.padding(32.dp), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = AccentTeal, modifier = Modifier.size(48.dp))
                            Text("Permiso de ubicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("HomeNavigator necesita tu ubicación para calcular la ruta hacia casa.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { perms.launchMultiplePermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)) {
                                Text("Conceder permiso", color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteInfoCard(routeInfo: RouteInfo, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f km".format(routeInfo.distanceKm), style = MaterialTheme.typography.titleLarge,
                    color = AccentTeal, fontWeight = FontWeight.ExtraBold)
                Text("distancia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val m = routeInfo.durationMinutes.roundToInt()
                Text(if (m >= 60) "${m/60}h ${m%60}min" else "${m}min", style = MaterialTheme.typography.titleLarge,
                    color = AccentAmber, fontWeight = FontWeight.ExtraBold)
                Text("tiempo estimado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Cerrar", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StepsPanel(routeInfo: RouteInfo, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Instrucciones (${routeInfo.steps.size} pasos)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Cerrar", Modifier.size(18.dp)) }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                items(routeInfo.steps) { step ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(AccentTeal))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(step.instruction, style = MaterialTheme.typography.bodyMedium)
                            if (step.distanceKm > 0.01)
                                Text("%.0f m".format(step.distanceKm * 1000),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}