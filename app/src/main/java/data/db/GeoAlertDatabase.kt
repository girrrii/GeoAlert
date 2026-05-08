package com.giri.geoalert.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.giri.geoalert.data.model.AlertTrigger
import com.giri.geoalert.data.model.GeoAlert

@Database(entities = [GeoAlert::class, AlertTrigger::class], version = 4, exportSchema = false)
abstract class GeoAlertDatabase : RoomDatabase() {

    abstract fun geoAlertDao(): GeoAlertDao
    abstract fun alertTriggerDao(): AlertTriggerDao

    companion object {
        @Volatile
        private var INSTANCE: GeoAlertDatabase? = null

        fun getDatabase(context: Context): GeoAlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoAlertDatabase::class.java,
                    "geoalert_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}