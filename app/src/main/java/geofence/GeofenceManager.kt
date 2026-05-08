package com.giri.geoalert.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.giri.geoalert.data.model.GeoAlert

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(alert: GeoAlert) {
        val geofence = Geofence.Builder()
            .setRequestId(alert.id.toString())
            .setCircularRegion(alert.latitude, alert.longitude, alert.radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener {
                android.util.Log.d("GeofenceManager", "Geofence added: ${alert.id}")
            }
            .addOnFailureListener {
                android.util.Log.e("GeofenceManager", "Geofence failed: ${it.message}")
            }
    }

    fun removeGeofence(alertId: Int) {
        geofencingClient.removeGeofences(listOf(alertId.toString()))
    }
}