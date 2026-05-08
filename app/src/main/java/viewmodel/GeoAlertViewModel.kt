package com.giri.geoalert.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giri.geoalert.data.db.GeoAlertDatabase
import com.giri.geoalert.data.model.GeoAlert
import com.giri.geoalert.data.network.NominatimClient
import com.giri.geoalert.data.network.NominatimResult
import com.giri.geoalert.geofence.GeofenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class GeoAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = GeoAlertDatabase.getDatabase(application).geoAlertDao()
    private val triggerDao = GeoAlertDatabase.getDatabase(application).alertTriggerDao()
    private val geofenceManager = GeofenceManager(application)

    val alerts = dao.getAllAlerts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchResults = MutableStateFlow<List<NominatimResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    val triggers = triggerDao.getAllTriggers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addAlert(alert: GeoAlert) {
        viewModelScope.launch {
            val id = dao.insertAlert(alert)
            val alertWithId = alert.copy(id = id.toInt())
            geofenceManager.addGeofence(alertWithId)
        }
    }

    fun deleteAlert(alert: GeoAlert) {
        viewModelScope.launch {
            dao.deleteAlert(alert)
            geofenceManager.removeGeofence(alert.id)
        }
    }

    fun updateAlert(alert: GeoAlert) {
        viewModelScope.launch {
            dao.updateAlert(alert)
            if (alert.isActive) {
                geofenceManager.addGeofence(alert)
            } else {
                geofenceManager.removeGeofence(alert.id)
            }
        }
    }

    fun searchLocation(query: String) {
        viewModelScope.launch {
            try {
                val results = NominatimClient.api.search(query)
                _searchResults.value = results
            } catch (e: Exception) {
                android.util.Log.e("GeoAlertViewModel", "Search failed: ${e.message}")
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun clearHistory() {
        viewModelScope.launch {
            triggerDao.clearAll()
        }
    }
    fun testNotification(context: android.content.Context) {
        viewModelScope.launch {
            val alertsList = dao.getAllAlerts().first()
            if (alertsList.isNotEmpty()) {
                val alert = alertsList.first()
                val message = if (alert.message.isNotBlank()) alert.message else "Entered zone: ${alert.name}"

                triggerDao.insertTrigger(
                    com.giri.geoalert.data.model.AlertTrigger(
                        alertName = alert.name,
                        message = message,
                        transition = "Entered"
                    )
                )

                val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channelId = "geoalert_channel"
                val channel = android.app.NotificationChannel(channelId, "GeoAlert Notifications", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                notificationManager.createNotificationChannel(channel)

                val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                    .setContentTitle("GeoAlert 📍 - ${alert.name}")
                    .setContentText(message)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 200, 500))
                    .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_SOUND)

                if (alert.phoneNumber.isNotBlank()) {
                    val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                        data = android.net.Uri.parse("tel:${alert.phoneNumber}")
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val callPendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, callIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(android.R.drawable.sym_action_call, "📞 Call", callPendingIntent)
                }

                notificationManager.notify(alert.name.hashCode(), builder.build())

                // Auto-call if trigger type is call or both
                if (alert.triggerType == "call" || alert.triggerType == "both") {
                    if (alert.phoneNumber.isNotBlank()) {
                        val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                            data = android.net.Uri.parse("tel:${alert.phoneNumber}")
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(callIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("GeoAlertViewModel", "Call failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}