package com.giri.geoalert.ui.screens

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giri.geoalert.data.model.GeoAlert
import com.giri.geoalert.data.network.NominatimResult
import com.giri.geoalert.viewmodel.GeoAlertViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import com.google.gson.JsonElement
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun MapScreen(viewModel: GeoAlertViewModel = viewModel()) {
    val alerts by viewModel.alerts.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(13.0827, 80.2707))
                    mapView = this

                    val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            selectedPoint = p
                            showDialog = true
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint) = false
                    })
                    overlays.add(eventsOverlay)
                }
            },
            update = { map ->
                map.overlays.removeIf { it is Marker || it is Polygon }

                alerts.forEach { alert ->
                    if (!alert.isActive) return@forEach

                    val marker = Marker(map).apply {
                        position = GeoPoint(alert.latitude, alert.longitude)
                        title = alert.name
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(marker)

                    val circle = Polygon().apply {
                        points = Polygon.pointsAsCircle(
                            GeoPoint(alert.latitude, alert.longitude),
                            alert.radius.toDouble()
                        )
                        fillPaint.apply {
                            color = android.graphics.Color.argb(50, 255, 0, 0)
                            style = Paint.Style.FILL
                        }
                        outlinePaint.apply {
                            color = android.graphics.Color.RED
                            strokeWidth = 2f
                        }
                    }
                    map.overlays.add(circle)
                }
                map.invalidate()
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search location...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = {
                            viewModel.searchLocation(searchQuery)
                        }) { Text("Go") }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            )

            // Search results dropdown
            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(searchResults) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = {
                                    val point = GeoPoint(
                                        result.lat.toDouble(),
                                        result.lon.toDouble()
                                    )
                                    mapView?.controller?.animateTo(point)
                                    mapView?.controller?.setZoom(13.0)

                                    // Draw boundary if available
                                    result.geojson?.let { geo ->
                                        try {
                                            val polygon = Polygon()
                                            polygon.outlinePaint.color = android.graphics.Color.BLUE
                                            polygon.outlinePaint.strokeWidth = 3f
                                            polygon.fillPaint.color = android.graphics.Color.argb(30, 0, 0, 255)

                                            val coords = geo.coordinates?.asJsonArray
                                            val outerRing = when (geo.type) {
                                                "Polygon" -> coords?.get(0)?.asJsonArray
                                                "MultiPolygon" -> coords?.get(0)?.asJsonArray?.get(0)?.asJsonArray
                                                else -> null
                                            }

                                            val points = outerRing?.mapNotNull { coord ->
                                                val arr = coord.asJsonArray
                                                GeoPoint(arr[1].asDouble, arr[0].asDouble)
                                            }

                                            if (!points.isNullOrEmpty()) {
                                                polygon.points = points
                                                mapView?.overlays?.add(polygon)
                                                mapView?.invalidate()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("MapScreen", "Boundary error: ${e.message}")
                                        }
                                    }

                                    viewModel.clearSearch()
                                    searchQuery = result.display_name.split(",").first()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        // Current location button
        FloatingActionButton(
            onClick = {
                getCurrentLocation(context) { lat, lon ->
                    val point = GeoPoint(lat, lon)
                    mapView?.controller?.animateTo(point)
                    mapView?.controller?.setZoom(16.0)

                    // Show blue dot for current location
                    mapView?.overlays?.removeIf { it is Marker && (it as Marker).title == "My Location" }
                    val myLocationMarker = Marker(mapView).apply {
                        position = point
                        title = "My Location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = androidx.core.content.ContextCompat.getDrawable(
                            context,
                            android.R.drawable.presence_online
                        )?.apply {
                            setTint(android.graphics.Color.BLUE)
                        }
                    }
                    mapView?.overlays?.add(myLocationMarker)
                    mapView?.invalidate()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 70.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "📍",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Bottom hint
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 4.dp
        ) {
            Text(
                text = "Tap on map to add alert",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    if (showDialog && selectedPoint != null) {
        AddAlertDialog(
            onConfirm = { name, radius, message, triggerType, phoneNumber ->
                viewModel.addAlert(
                    GeoAlert(
                        name = name,
                        latitude = selectedPoint!!.latitude,
                        longitude = selectedPoint!!.longitude,
                        radius = radius,
                        message = message,
                        triggerType = triggerType,
                        phoneNumber = phoneNumber
                    )
                )
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun SearchResultItem(result: NominatimResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = result.display_name.split(",").first(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = result.display_name.split(",").drop(1).take(2).joinToString(","),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AddAlertDialog(
    onConfirm: (String, Float, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var message by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf("notification") }
    var phoneNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Alert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Alert Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (meters)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Custom Message (optional)") },
                    singleLine = true
                )

                Text("Trigger Type:", style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = triggerType == "notification",
                        onClick = { triggerType = "notification" },
                        label = { Text("📳 Notify") }
                    )
                    FilterChip(
                        selected = triggerType == "call",
                        onClick = { triggerType = "call" },
                        label = { Text("📞 Call") }
                    )
                    FilterChip(
                        selected = triggerType == "both",
                        onClick = { triggerType = "both" },
                        label = { Text("Both") }
                    )
                }

                if (triggerType == "call" || triggerType == "both") {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(name, radius.toFloatOrNull() ?: 100f, message, triggerType, phoneNumber)
                }
            }) { Text("Add Alert") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            location?.let {
                onLocation(it.latitude, it.longitude)
            }
        }
}