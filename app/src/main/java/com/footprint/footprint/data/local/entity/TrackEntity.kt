package com.footprint.footprint.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distance: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
