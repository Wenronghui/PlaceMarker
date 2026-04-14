package com.footprint.footprint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.footprint.footprint.domain.model.Converters
import com.footprint.footprint.domain.model.Marker
import com.footprint.footprint.domain.model.Track
import com.footprint.footprint.domain.model.TrackPoint

@Database(
    entities = [Marker::class, Track::class, TrackPoint::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao
    
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
