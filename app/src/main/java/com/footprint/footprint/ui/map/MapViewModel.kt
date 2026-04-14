package com.footprint.footprint.ui.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footprint.footprint.data.local.AppDatabase
import com.footprint.footprint.data.local.entity.MarkerEntity
import com.footprint.footprint.data.local.entity.TrackEntity
import com.footprint.footprint.data.local.entity.TrackPointEntity
import com.footprint.footprint.data.repository.MarkerRepository
import com.footprint.footprint.data.repository.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import kotlin.math.*

data class MapUiState(
    // 位置
    val currentLocation: GeoPoint? = null,
    val isLocationEnabled: Boolean = false,
    
    // 地图状态
    val centerLat: Double = 35.0,
    val centerLon: Double = 105.0,
    val zoomLevel: Double = 10.0,
    val currentLayer: MapLayerType = MapLayerType.STANDARD,
    
    // 离线地图
    val offlineMapAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val offlineRegions: List<OfflineRegion> = emptyList(),
    
    // 标记
    val markers: List<MarkerEntity> = emptyList(),
    
    // 轨迹记录
    val isTracking: Boolean = false,
    val currentTrack: TrackEntity? = null,
    val trackPoints: List<TrackPointEntity> = emptyList(),
    val trackDistance: Double = 0.0
)

data class OfflineRegion(
    val id: String,
    val name: String,
    val center: GeoPoint,
    val zoomRange: IntRange,
    val size: Long,
    val downloaded: Boolean = false
)

// 地图图层配置
enum class MapLayerType(
    val displayName: String,
    val tileSource: org.osmdroid.tileprovider.tilesource.ITileSource,
    val description: String
) {
    // 标准地图
    STANDARD(
        "标准地图",
        TileSourceFactory.MAPNIK,
        "通用道路地图"
    ),
    // 卫星地图
    SATELLITE(
        "卫星影像",
        TileSourceFactory.USGS_SAT,
        "卫星航拍图像"
    ),
    // 徒步/地形图
    HIKING(
        "徒步地形",
        TileSourceFactory.OpenTopo,
        "等高线地形图"
    ),
    // 水域/钓鱼专用地图（使用OpenTopo，有水体标注）
    WATER(
        "水域钓场",
        TileSourceFactory.OpenTopo,
        "水库河流湖泊标注，适合钓鱼"
    ),
    // 户外地图（使用OpenTopo）
    OUTDOOR(
        "户外探索",
        TileSourceFactory.OpenTopo,
        "户外探险地图"
    )
}

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val markerRepository: MarkerRepository
    private val trackRepository: TrackRepository
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    init {
        val database = AppDatabase.getInstance(application)
        markerRepository = MarkerRepository(database.markerDao())
        trackRepository = TrackRepository(database.trackDao())
        
        // 加载标记
        viewModelScope.launch {
            markerRepository.getAllMarkers().collect { markers ->
                _uiState.update { it.copy(markers = markers) }
            }
        }
        
        // 检查是否有活动轨迹
        viewModelScope.launch(Dispatchers.IO) {
            val activeTrack = trackRepository.getActiveTrack()
            if (activeTrack != null) {
                val points = trackRepository.getPointsByTrackIdSync(activeTrack.id)
                _uiState.update { 
                    it.copy(
                        isTracking = true,
                        currentTrack = activeTrack,
                        trackPoints = points,
                        trackDistance = calculateDistance(points)
                    )
                }
            }
        }
        
        // 加载预置区域
        loadPresetRegions()
    }
    
    private fun loadPresetRegions() {
        val regions = listOf(
            OfflineRegion("changjiang", "长江流域", GeoPoint(30.0, 112.0), 6..12, 500_000_000),
            OfflineRegion("huanghe", "黄河流域", GeoPoint(35.0, 105.0), 6..12, 450_000_000),
            OfflineRegion("dongtinghu", "洞庭湖", GeoPoint(29.0, 112.0), 10..16, 80_000_000),
            OfflineRegion("poyanghu", "鄱阳湖", GeoPoint(29.0, 116.0), 10..16, 80_000_000),
            OfflineRegion("sanxia", "三峡水库", GeoPoint(31.0, 111.0), 8..14, 100_000_000),
            OfflineRegion("taihu", "太湖流域", GeoPoint(31.0, 120.0), 9..15, 90_000_000),
            OfflineRegion("qinhai", "青海湖", GeoPoint(37.0, 100.0), 9..14, 60_000_000),
            OfflineRegion("yilonghu", "溢龙湖", GeoPoint(33.5, 116.0), 10..16, 50_000_000)
        )
        _uiState.update { it.copy(offlineRegions = regions) }
    }
    
    fun getPresetRegions(): List<OfflineRegion> = _uiState.value.offlineRegions
    
    fun startLocation() {
        _uiState.update { it.copy(isLocationEnabled = true) }
    }
    
    fun updateLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        _uiState.update { it.copy(currentLocation = geoPoint) }
        
        // 如果正在记录轨迹，添加点
        if (_uiState.value.isTracking) {
            addTrackPoint(geoPoint)
        }
    }
    
    fun updateCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLon = lon) }
    }
    
    fun updateZoom(zoom: Double) {
        _uiState.update { it.copy(zoomLevel = zoom) }
    }
    
    fun setMapLayer(layer: MapLayerType) {
        _uiState.update { it.copy(currentLayer = layer) }
    }
    
    // 标记功能
    fun addMarker(lat: Double, lon: Double, title: String, description: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val marker = MarkerEntity(
                title = title,
                description = description,
                latitude = lat,
                longitude = lon,
                category = category
            )
            markerRepository.insertMarker(marker)
        }
    }
    
    fun deleteMarker(marker: MarkerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            markerRepository.deleteMarker(marker)
        }
    }
    
    // 轨迹记录功能
    fun startTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val track = TrackEntity(
                name = "轨迹 ${System.currentTimeMillis() % 10000}",
                startTime = System.currentTimeMillis(),
                isActive = true
            )
            val trackId = trackRepository.insertTrack(track)
            val newTrack = track.copy(id = trackId)
            
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        isTracking = true,
                        currentTrack = newTrack,
                        trackPoints = emptyList(),
                        trackDistance = 0.0
                    )
                }
            }
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
                
                withContext(Dispatchers.Main) {
                    _uiState.update { 
                        it.copy(
                            isTracking = false, 
                            currentTrack = null, 
                            trackPoints = emptyList(), 
                            trackDistance = 0.0
                        ) 
                    }
                }
            }
        }
    }
    
    private fun addTrackPoint(point: GeoPoint) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = _uiState.value.currentTrack ?: return@launch
            
            val trackPoint = TrackPointEntity(
                trackId = track.id,
                latitude = point.latitude,
                longitude = point.longitude,
                timestamp = System.currentTimeMillis()
            )
            trackRepository.insertPoint(trackPoint)
            
            // 更新UI
            val newPoints = _uiState.value.trackPoints + trackPoint
            val newDistance = calculateDistance(newPoints)
            
            withContext(Dispatchers.Main) {
                _uiState.update { 
                    it.copy(
                        trackPoints = newPoints,
                        trackDistance = newDistance
                    )
                }
            }
        }
    }
    
    private fun calculateDistance(points: List<TrackPointEntity>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += haversine(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        return totalDistance
    }
    
    // Haversine公式计算两点间距离（米）
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // 地球半径（米）
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    // 离线地图下载（预留接口）
    fun downloadOfflineRegion(region: OfflineRegion) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f) }
            
            // 模拟下载进度
            for (i in 1..10) {
                kotlinx.coroutines.delay(500)
                _uiState.update { it.copy(downloadProgress = i / 10f) }
            }
            
            // 更新下载状态
            val updatedRegions = _uiState.value.offlineRegions.map {
                if (it.id == region.id) it.copy(downloaded = true) else it
            }
            _uiState.update { 
                it.copy(
                    isDownloading = false,
                    offlineRegions = updatedRegions,
                    offlineMapAvailable = true
                ) 
            }
        }
    }
}
