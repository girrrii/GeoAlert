package com.giri.geoalert.data.db

import androidx.room.*
import com.giri.geoalert.data.model.AlertTrigger
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertTriggerDao {

    @Insert
    suspend fun insertTrigger(trigger: AlertTrigger)

    @Query("SELECT * FROM alert_triggers ORDER BY timestamp DESC")
    fun getAllTriggers(): Flow<List<AlertTrigger>>

    @Query("DELETE FROM alert_triggers")
    suspend fun clearAll()
}