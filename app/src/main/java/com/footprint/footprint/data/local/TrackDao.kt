package com.footprint.footprint.data.local

import androidx.room.*
import com.footprint.footprint.data.local.entity.TrackEntity
import com.footprint.footprint.data.local.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY createdAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrack(): TrackEntity?
    
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long
    
    @Update
    suspend fun updateTrack(track: TrackEntity)
    
    @Delete
    suspend fun deleteTrack(track: TrackEntity)
    
    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Long)
    
    // Track points
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsByTrackId(trackId: Long): Flow<List<TrackPointEntity>>
    
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsByTrackIdSync(trackId: Long): List<TrackPointEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity): Long
    
    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deletePointsByTrackId(trackId: Long)
}
