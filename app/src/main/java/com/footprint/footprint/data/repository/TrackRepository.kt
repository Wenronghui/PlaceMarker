package com.footprint.footprint.data.repository

import com.footprint.footprint.data.local.TrackDao
import com.footprint.footprint.data.local.entity.TrackEntity
import com.footprint.footprint.data.local.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

class TrackRepository(private val trackDao: TrackDao) {
    fun getAllTracks(): Flow<List<TrackEntity>> = trackDao.getAllTracks()
    
    suspend fun getActiveTrack(): TrackEntity? = trackDao.getActiveTrack()
    
    suspend fun getTrackById(id: Long): TrackEntity? = trackDao.getTrackById(id)
    
    suspend fun insertTrack(track: TrackEntity): Long = trackDao.insertTrack(track)
    
    suspend fun updateTrack(track: TrackEntity) = trackDao.updateTrack(track)
    
    suspend fun deleteTrack(track: TrackEntity) = trackDao.deleteTrack(track)
    
    suspend fun deleteTrackById(id: Long) {
        trackDao.deletePointsByTrackId(id)
        trackDao.deleteTrackById(id)
    }
    
    // Track points
    fun getPointsByTrackId(trackId: Long): Flow<List<TrackPointEntity>> = 
        trackDao.getPointsByTrackId(trackId)
    
    suspend fun getPointsByTrackIdSync(trackId: Long): List<TrackPointEntity> = 
        trackDao.getPointsByTrackIdSync(trackId)
    
    suspend fun insertPoint(point: TrackPointEntity): Long = trackDao.insertPoint(point)
    
    suspend fun deletePointsByTrackId(trackId: Long) = trackDao.deletePointsByTrackId(trackId)
}
