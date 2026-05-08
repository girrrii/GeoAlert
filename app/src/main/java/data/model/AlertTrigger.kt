package com.giri.geoalert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_triggers")
data class AlertTrigger(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val alertName: String,
    val message: String,
    val transition: String,
    val timestamp: Long = System.currentTimeMillis()
)