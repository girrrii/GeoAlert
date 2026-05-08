package com.giri.geoalert.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.giri.geoalert.data.db.GeoAlertDatabase
import com.giri.geoalert.data.model.AlertTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        val transitionText = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited"
            else -> return
        }

        val db = GeoAlertDatabase.getDatabase(context)
        val dao = db.geoAlertDao()
        val triggerDao = db.alertTriggerDao()

        triggeringGeofences.forEach { geofence ->
            CoroutineScope(Dispatchers.IO).launch {
                val alerts = dao.getAllAlerts().first()
                val alert = alerts.find { it.id.toString() == geofence.requestId }
                val message = if (alert?.message?.isNotBlank() == true) {
                    alert.message
                } else {
                    "$transitionText zone: ${alert?.name ?: geofence.requestId}"
                }

                // Save to history
                triggerDao.insertTrigger(
                    AlertTrigger(
                        alertName = alert?.name ?: geofence.requestId,
                        message = message,
                        transition = transitionText
                    )
                )

                val triggerType = alert?.triggerType ?: "notification"
                val phoneNumber = alert?.phoneNumber ?: ""

                when (triggerType) {
                    "notification" -> {
                        sendNotification(context, alert?.name ?: geofence.requestId, message, null)
                    }
                    "call" -> {
                        makeCall(context, phoneNumber)
                    }
                    "both" -> {
                        sendNotification(context, alert?.name ?: geofence.requestId, message, phoneNumber)
                        makeCall(context, phoneNumber)
                    }
                }
            }
        }
    }

    private fun makeCall(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(callIntent)
        } catch (e: Exception) {
            // Fallback to dialer
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        }
    }

    private fun sendNotification(
        context: Context,
        alertName: String,
        message: String,
        phoneNumber: String?
    ) {
        val channelId = "geoalert_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "GeoAlert Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            enableLights(true)
            lightColor = android.graphics.Color.RED
        }
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("GeoAlert 📍 - $alertName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)

        // Add call button if phone number provided
        if (!phoneNumber.isNullOrBlank()) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                context, 0, callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.sym_action_call,
                "📞 Call",
                callPendingIntent
            )
        }

        notificationManager.notify(alertName.hashCode(), builder.build())
    }
}