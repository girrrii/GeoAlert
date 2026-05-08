package com.giri.geoalert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geo_alerts")
data class GeoAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val isActive: Boolean = true,
    val message: String = "",
    val triggerType: String = "notification", // "notification", "call", "both"
    val phoneNumber: String = ""
)