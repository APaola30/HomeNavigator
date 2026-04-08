package com.homenavigator.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.homenavigator.data.model.HomeAddress
import com.homenavigator.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val homeAddress   by viewModel.homeAddress.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val geocodeResult by viewModel.geocodeResult.collectAsState()
    val message       by viewModel.message.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context  = LocalContext.current

    var searchQuery      by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMapPicker    by remember { mutableStateOf(false) }
    var mapMarker        by remember { mutableStateOf<GeoPoint?>(null) }

    val initGeoPoint = remember(homeAddress.isConfigured, homeAddress.latitude, homeAddress.longitude) {
        if (homeAddress.isConfigured) homeAddress.geoPoint else GeoPoint(19.4326, -99.1332)
    }

    // MapView del selector — se crea una vez y se reutiliza
    val pickerMapView = rememberPickerMapView(initGeoPoint) { tapped ->
        mapMarker = tapped
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    // Cuando se geocodifica un resultado, animar la cámara del selector
    LaunchedEffect(geocodeResult) {
        (geocodeResult as? SettingsViewModel.GeoResult.Found)?.let {
            pickerMapView.controller.animateTo(it.geoPoint, 16.0, 700)
        }
    }

    // ── Selector en mapa ───────────────────────────────────────────────────
    if (showMapPicker) {
        Box(Modifier.fillMaxSize()) {

            // Actualizar overlays del selector cuando cambia el marcador
            LaunchedEffect(mapMarker, homeAddress.isConfigured) {
                pickerMapView.overlays.clear()
                if (homeAddress.isConfigured) {
                    pickerMapView.overlays.add(Marker(pickerMapView).apply {
                        position = homeAddress.geoPoint
                        title    = "Casa actual"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                }
                mapMarker?.let { gp ->
                    pickerMapView.overlays.add(Marker(pickerMapView).apply {
                        position = gp
                        title    = "Nueva casa"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                }
                pickerMapView.invalidate()
            }

            AndroidView(
                factory  = { pickerMapView },
                modifier = Modifier.fillMaxSize(),
                update   = {}   // overlays manejados por LaunchedEffect
            )

            // Barra superior del selector
            Surface(
                Modifier.fillMaxWidth().statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface.copy(0.95f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showMapPicker = false; mapMarker = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                    Text("Toca el mapa para marcar tu casa",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Indicador central cuando no hay marcador
            if (mapMarker == null) {
                Box(Modifier.align(Alignment.Center).padding(bottom = 40.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = DeepNavy.copy(0.8f)) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = AccentTeal,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Toca para marcar tu casa", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Botones confirmar / cancelar
            AnimatedVisibility(
                mapMarker != null,
                Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Surface(
                    Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { mapMarker = null }, Modifier.weight(1f)) {
                            Icon(Icons.Default.Clear, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                mapMarker?.let {
                                    viewModel.saveHomeFromMap(it)
                                    showMapPicker = false
                                    mapMarker = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(AccentTeal),
                            enabled = !isLoading
                        ) {
                            if (isLoading)
                                CircularProgressIndicator(Modifier.size(16.dp),
                                    strokeWidth = 2.dp, color = DeepNavy)
                            else {
                                Icon(Icons.Default.Check, null, tint = DeepNavy)
                                Spacer(Modifier.width(4.dp))
                                Text("Confirmar", color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // ── Diálogo eliminar ───────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
            title = { Text("Eliminar dirección") },
            text = { Text("¿Eliminar la dirección de casa guardada?") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHome(); showDeleteDialog = false }) {
                    Text("Eliminar", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── UI principal ───────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar casa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CurrentHomeCard(homeAddress) { showDeleteDialog = true }
            HorizontalDivider()

            Text("Buscar por dirección", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Dirección") },
                placeholder = { Text("Ej: Av. Reforma 222, CDMX") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AccentTeal) },
                trailingIcon = {
                    if (searchQuery.isNotBlank())
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide(); viewModel.searchAddress(searchQuery)
                }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = { keyboard?.hide(); viewModel.searchAddress(searchQuery) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(AccentTeal),
                enabled = searchQuery.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = DeepNavy)
                    Spacer(Modifier.width(8.dp))
                    Text("Buscando…", color = DeepNavy)
                } else {
                    Icon(Icons.Default.Search, null, tint = DeepNavy)
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar", color = DeepNavy, fontWeight = FontWeight.Bold)
                }
            }

            when (val r = geocodeResult) {
                is SettingsViewModel.GeoResult.Found ->
                    GeoResultCard(r.displayName, r.geoPoint) { viewModel.confirmSaveGeocodeResult() }
                is SettingsViewModel.GeoResult.Error ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(ErrorRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = ErrorRed)
                            Spacer(Modifier.width(8.dp))
                            Text(r.msg, color = ErrorRed)
                        }
                    }
                else -> Unit
            }

            HorizontalDivider()
            Text("Seleccionar en mapa", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            FilledTonalButton(
                onClick = { showMapPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text("Abrir selector en mapa")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MapView del selector — se crea una vez, detecta taps correctamente
// usando un IMapEventsOverlay en lugar de setOnTouchListener crudo
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun rememberPickerMapView(
    initCenter: GeoPoint,
    onTap: (GeoPoint) -> Unit
): MapView {
    val context = LocalContext.current
    // Guardamos onTap en un ref para no recrear el MapView cuando cambia el lambda
    val onTapRef = rememberUpdatedState(onTap)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(14.0)
            controller.setCenter(initCenter)

            // Usar MapEventsOverlay para detectar taps de forma correcta
            // sin bloquear el hilo principal con setOnTouchListener
            overlays.add(
                object : org.osmdroid.views.overlay.Overlay() {
                    override fun onSingleTapConfirmed(
                        e: android.view.MotionEvent?,
                        mapView: MapView?
                    ): Boolean {
                        e ?: return false
                        mapView ?: return false
                        val geoPoint = mapView.projection.fromPixels(
                            e.x.toInt(), e.y.toInt()
                        ) as GeoPoint
                        onTapRef.value(geoPoint)
                        return true
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    return mapView
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de casa actual
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CurrentHomeCard(homeAddress: HomeAddress, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            if (homeAddress.isConfigured) AccentTeal.copy(0.12f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (homeAddress.isConfigured) AccentTeal
                    else MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home, null,
                    tint = if (homeAddress.isConfigured) DeepNavy
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (homeAddress.isConfigured) "Casa configurada" else "Sin dirección",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (homeAddress.isConfigured) AccentTeal
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (homeAddress.isConfigured) {
                    Text(homeAddress.displayName, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${"%.5f".format(homeAddress.latitude)}, ${"%.5f".format(homeAddress.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Configura tu casa para poder navegar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (homeAddress.isConfigured)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = ErrorRed)
                }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de resultado de geocodificación con mini-mapa preview
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GeoResultCard(
    displayName: String,
    geoPoint: GeoPoint,
    onConfirm: () -> Unit
) {
    // Mini MapView de solo lectura para el preview
    val previewMapView = rememberPreviewMapView(geoPoint)

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = AccentTeal)
                Spacer(Modifier.width(8.dp))
                Text("Resultado encontrado", style = MaterialTheme.typography.labelLarge,
                    color = AccentTeal, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${"%.5f".format(geoPoint.latitude)}, ${"%.5f".format(geoPoint.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Mini mapa preview
            Box(
                Modifier.fillMaxWidth().height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory  = { previewMapView },
                    modifier = Modifier.fillMaxSize(),
                    update   = {}
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConfirm, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(AccentTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Home, null, tint = DeepNavy)
                Spacer(Modifier.width(6.dp))
                Text("Guardar como mi casa", color = DeepNavy, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun rememberPreviewMapView(geoPoint: GeoPoint): MapView {
    val context = LocalContext.current
    val mapView = remember(geoPoint) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            isClickable = false
            isTilesScaledToDpi = true
            controller.setZoom(15.0)
            controller.setCenter(geoPoint)
            overlays.add(Marker(this).apply {
                position = geoPoint
                title    = "Casa"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
    }
    DisposableEffect(geoPoint) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }
    return mapView
}
