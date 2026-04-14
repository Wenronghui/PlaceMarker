package com.footprint.footprint.data.local

import androidx.room.*
import com.footprint.footprint.domain.model.Marker
import com.footprint.footprint.domain.model.Track
import com.footprint.footprint.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers ORDER BY createdAt DESC")
    fun getAllMarkers(): Flow<List<Marker>>
    
    @Query("SELECT * FROM markers WHERE id = :id")
    suspend fun getMarkerById(id: Long): Marker?
    
    @Query("SELECT * FROM markers WHERE category = :category ORDER BY createdAt DESC")
    fun getMarkersByCategory(category: String): Flow<List<Marker>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: Marker): Long
    
    @Update
    suspend fun updateMarker(marker: Marker)
    
    @Delete
    suspend fun deleteMarker(marker: Marker)
    
    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteMarkerById(id: Long)
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracks(): Flow<List<Track>>
    
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): Track?
    
    @Query("SELECT * FROM tracks WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrack(): Track?
    
    @Query("SELECT * FROM tracks WHERE isActive = 1 LIMIT 1")
    fun getActiveTrackFlow(): Flow<Track?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long
    
    @Update
    suspend fun updateTrack(track: Track)
    
    @Delete
    suspend fun deleteTrack(track: Track)
}

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsByTrackId(trackId: Long): Flow<List<TrackPoint>>
    
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsByTrackIdSync(trackId: Long): List<TrackPoint>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPoint): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPoint>)
    
    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deletePointsByTrackId(trackId: Long)
}
