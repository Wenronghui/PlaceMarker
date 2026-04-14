package com.footprint.footprint.data.repository

import com.footprint.footprint.data.local.MarkerDao
import com.footprint.footprint.data.local.entity.MarkerEntity
import kotlinx.coroutines.flow.Flow

class MarkerRepository(private val markerDao: MarkerDao) {
    fun getAllMarkers(): Flow<List<MarkerEntity>> = markerDao.getAllMarkers()
    
    suspend fun getMarkerById(id: Long): MarkerEntity? = markerDao.getMarkerById(id)
    
    fun getMarkersByCategory(category: String): Flow<List<MarkerEntity>> = 
        markerDao.getMarkersByCategory(category)
    
    suspend fun insertMarker(marker: MarkerEntity): Long = markerDao.insertMarker(marker)
    
    suspend fun updateMarker(marker: MarkerEntity) = markerDao.updateMarker(marker)
    
    suspend fun deleteMarker(marker: MarkerEntity) = markerDao.deleteMarker(marker)
    
    suspend fun deleteMarkerById(id: Long) = markerDao.deleteMarkerById(id)
}
