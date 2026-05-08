package com.giri.geoalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.giri.geoalert.data.model.GeoAlert
import com.giri.geoalert.viewmodel.GeoAlertViewModel

@Composable
fun AlertListScreen(viewModel: GeoAlertViewModel) {
    val alerts by viewModel.alerts.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "My Alerts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alerts yet. Tap on the map to add one!")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts) { alert ->
                    AlertItem(
                        alert = alert,
                        onDelete = { viewModel.deleteAlert(alert) },
                        onToggle = { isActive ->
                            viewModel.updateAlert(alert.copy(isActive = isActive))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: GeoAlert, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = alert.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Radius: ${alert.radius.toInt()}m",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Lat: ${"%.4f".format(alert.latitude)}, Lng: ${"%.4f".format(alert.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = alert.isActive,
                onCheckedChange = { onToggle(it) }
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Alert")
            }
        }
    }
}