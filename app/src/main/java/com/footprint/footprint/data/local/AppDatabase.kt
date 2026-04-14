package com.footprint.footprint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.footprint.footprint.data.local.entity.MarkerEntity
import com.footprint.footprint.data.local.entity.TrackEntity
import com.footprint.footprint.data.local.entity.TrackPointEntity

@Database(
    entities = [
        MarkerEntity::class,
        TrackEntity::class,
        TrackPointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
    abstract fun trackDao(): TrackDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "footprint_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
