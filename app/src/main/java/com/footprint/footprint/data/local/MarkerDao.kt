package com.footprint.footprint.data.local

import androidx.room.*
import com.footprint.footprint.data.local.entity.MarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers ORDER BY createdAt DESC")
    fun getAllMarkers(): Flow<List<MarkerEntity>>
    
    @Query("SELECT * FROM markers WHERE id = :id")
    suspend fun getMarkerById(id: Long): MarkerEntity?
    
    @Query("SELECT * FROM markers WHERE category = :category ORDER BY createdAt DESC")
    fun getMarkersByCategory(category: String): Flow<List<MarkerEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: MarkerEntity): Long
    
    @Update
    suspend fun updateMarker(marker: MarkerEntity)
    
    @Delete
    suspend fun deleteMarker(marker: MarkerEntity)
    
    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteMarkerById(id: Long)
}
