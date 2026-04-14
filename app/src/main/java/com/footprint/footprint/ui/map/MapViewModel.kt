package com.footprint.footprint.ui.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footprint.footprint.data.local.AppDatabase
import com.footprint.footprint.data.repository.MarkerRepository
import com.footprint.footprint.data.repository.TrackRepository
import com.footprint.footprint.domain.model.Marker
import com.footprint.footprint.domain.model.MarkerCategory
import com.footprint.footprint.domain.model.Track
import com.footprint.footprint.domain.model.TrackPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

data class MapUiState(
    val markers: List<Marker> = emptyList(),
    val trackPoints: List<TrackPoint> = emptyList(),
    val currentLocation: GeoPoint? = null,
    val currentLayer: MapLayer = MapLayer.STANDARD,
    val isTracking: Boolean = false,
    val currentTrack: Track? = null,
    val trackDistance: Double = 0.0
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val markerRepository = MarkerRepository(database.markerDao())
    private val trackRepository = TrackRepository(database.trackDao(), database.trackPointDao())
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(application)
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    
    init {
        // Observe markers
        viewModelScope.launch {
            markerRepository.getAllMarkers().collect { markers ->
                _uiState.update { it.copy(markers = markers) }
            }
        }
        
        // Check for active track
        viewModelScope.launch {
            trackRepository.getActiveTrackFlow().collect { track ->
                if (track != null) {
                    _uiState.update { it.copy(isTracking = true, currentTrack = track) }
                    // Load track points
                    trackRepository.getPointsByTrackId(track.id).collect { points ->
                        val distance = calculateDistance(points)
                        _uiState.update { 
                            it.copy(trackPoints = points, trackDistance = distance) 
                        }
                    }
                } else {
                    _uiState.update { it.copy(isTracking = false, currentTrack = null, trackPoints = emptyList(), trackDistance = 0.0) }
                }
            }
        }
        
        // Start location updates
        startLocationUpdates()
    }
    
    private fun startLocationUpdates() {
        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        _uiState.update { 
                            it.copy(currentLocation = GeoPoint(location.latitude, location.longitude))
                        }
                        
                        // If tracking, save point
                        if (_uiState.value.isTracking) {
                            viewModelScope.launch(Dispatchers.IO) {
                                val track = _uiState.value.currentTrack
                                if (track != null) {
                                    val point = TrackPoint(
                                        trackId = track.id,
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        altitude = if (location.hasAltitude()) location.altitude else null,
                                        timestamp = System.currentTimeMillis(),
                                        accuracy = if (location.hasAccuracy()) location.accuracy else null
                                    )
                                    trackRepository.insertPoint(point)
                                }
                            }
                        }
                    }
                }
            }
            
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L // 5 seconds
            ).setMinUpdateIntervalMillis(2000L).build()
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                null
            )
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }
    
    fun setMapLayer(layer: MapLayer) {
        _uiState.update { it.copy(currentLayer = layer) }
    }
    
    fun addMarker(
        name: String,
        description: String,
        category: MarkerCategory,
        location: GeoPoint
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val marker = Marker(
                name = name,
                description = description,
                latitude = location.latitude,
                longitude = location.longitude,
                category = category
            )
            markerRepository.insertMarker(marker)
        }
    }
    
    fun startTracking(name: String = "轨迹记录") {
        viewModelScope.launch(Dispatchers.IO) {
            val track = Track(
                name = name,
                isActive = true
            )
            trackRepository.insertTrack(track)
        }
    }
    
    fun stopTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val track = _uiState.value.currentTrack
            if (track != null) {
                val points = trackRepository.getPointsByTrackIdSync(track.id)
                val distance = calculateDistance(points)
                
                trackRepository.updateTrack(
                    track.copy(
                        isActive = false,
                        endTime = System.currentTimeMillis(),
                        distance = distance
                    )
                )
            }
        }
    }
    
    fun deleteMarker(marker: Marker) {
        viewModelScope.launch(Dispatchers.IO) {
            markerRepository.deleteMarker(marker)
        }
    }
    
    private fun calculateDistance(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            totalDistance += calculateHaversineDistance(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude
            )
        }
        return totalDistance
    }
    
    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
    
    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
}
