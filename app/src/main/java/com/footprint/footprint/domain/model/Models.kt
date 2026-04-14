package com.footprint.footprint.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class MarkerCategory {
    CAMP,       // 营地
    PEAK,       // 山峰
    WATER,      // 水源
    VIEW,       // 观景点
    PARKING,    // 停车点
    OTHER       // 其他
}

@Entity(tableName = "markers")
@TypeConverters(Converters::class)
data class Marker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val category: MarkerCategory = MarkerCategory.OTHER,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val distance: Double = 0.0, // meters
    val isActive: Boolean = false
)

@Entity(tableName = "track_points")
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float? = null
)

class Converters {
    @TypeConverter
    fun fromMarkerCategory(value: MarkerCategory): String = value.name

    @TypeConverter
    fun toMarkerCategory(value: String): MarkerCategory = MarkerCategory.valueOf(value)
}
