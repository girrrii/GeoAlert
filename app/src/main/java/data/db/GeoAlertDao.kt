package com.giri.geoalert.data.db

import androidx.room.*
import com.giri.geoalert.data.model.GeoAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: GeoAlert): Long

    @Delete
    suspend fun deleteAlert(alert: GeoAlert)

    @Query("SELECT * FROM geo_alerts")
    fun getAllAlerts(): Flow<List<GeoAlert>>

    @Update
    suspend fun updateAlert(alert: GeoAlert)
}