package com.footprint.footprint.data.repository

import com.footprint.footprint.data.local.MarkerDao
import com.footprint.footprint.data.local.TrackDao
import com.footprint.footprint.data.local.TrackPointDao
import com.footprint.footprint.domain.model.Marker
import com.footprint.footprint.domain.model.Track
import com.footprint.footprint.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

class MarkerRepository(private val markerDao: MarkerDao) {
    fun getAllMarkers(): Flow<List<Marker>> = markerDao.getAllMarkers()
    
    suspend fun getMarkerById(id: Long): Marker? = markerDao.getMarkerById(id)
    
    fun getMarkersByCategory(category: String): Flow<List<Marker>> = 
        markerDao.getMarkersByCategory(category)
    
    suspend fun insertMarker(marker: Marker): Long = markerDao.insertMarker(marker)
    
    suspend fun updateMarker(marker: Marker) = markerDao.updateMarker(marker)
    
    suspend fun deleteMarker(marker: Marker) = markerDao.deleteMarker(marker)
    
    suspend fun deleteMarkerById(id: Long) = markerDao.deleteMarkerById(id)
}

class TrackRepository(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao
) {
    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()
    
    suspend fun getTrackById(id: Long): Track? = trackDao.getTrackById(id)
    
    suspend fun getActiveTrack(): Track? = trackDao.getActiveTrack()
    
    fun getActiveTrackFlow(): Flow<Track?> = trackDao.getActiveTrackFlow()
    
    suspend fun insertTrack(track: Track): Long = trackDao.insertTrack(track)
    
    suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)
    
    suspend fun deleteTrack(track: Track) {
        trackPointDao.deletePointsByTrackId(track.id)
        trackDao.deleteTrack(track)
    }
    
    fun getPointsByTrackId(trackId: Long): Flow<List<TrackPoint>> = 
        trackPointDao.getPointsByTrackId(trackId)
    
    suspend fun getPointsByTrackIdSync(trackId: Long): List<TrackPoint> = 
        trackPointDao.getPointsByTrackIdSync(trackId)
    
    suspend fun insertPoint(point: TrackPoint): Long = trackPointDao.insertPoint(point)
    
    suspend fun insertPoints(points: List<TrackPoint>) = trackPointDao.insertPoints(points)
}
