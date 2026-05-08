package com.giri.geoalert.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.giri.geoalert.data.db.GeoAlertDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = GeoAlertDatabase.getDatabase(context)
            val geofenceManager = GeofenceManager(context)

            CoroutineScope(Dispatchers.IO).launch {
                val alerts = db.geoAlertDao().getAllAlerts().first()
                alerts.filter { it.isActive }.forEach { alert ->
                    geofenceManager.addGeofence(alert)
                }
            }
        }
    }
}