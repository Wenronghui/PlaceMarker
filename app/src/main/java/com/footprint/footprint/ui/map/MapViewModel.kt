package com.footprint.footprint.ui.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint

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
    // 户外地图
    OUTDOOR(
        "户外探索",
        TileSourceFactory.US_Topo,
        "户外探险地图"
    ),
    // 暖色地图
    CYCLE(
        "骑行地图",
        TileSourceFactory.CYCLEMAP,
        "骑行友好地图"
    )
}

// UI状态
data class PureMapUiState(
    val currentLocation: GeoPoint? = null,
    val currentLayer: MapLayerType = MapLayerType.STANDARD,
    val centerLat: Double = 35.0,
    val centerLon: Double = 105.0,
    val zoomLevel: Double = 10.0,
    val isLocationEnabled: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val offlineMapAvailable: Boolean = false,
    // 离线地图区域
    val offlineRegions: List<OfflineRegion> = emptyList()
)

// 离线地图区域
data class OfflineRegion(
    val id: String,
    val name: String,
    val center: GeoPoint,
    val zoomMin: Int,
    val zoomMax: Int,
    val size: Long, // bytes
    val downloaded: Boolean = false
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(application)
    
    private val _uiState = MutableStateFlow(PureMapUiState())
    val uiState: StateFlow<PureMapUiState> = _uiState.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    
    init {
        // 检查离线地图可用性
        checkOfflineMapAvailability()
    }
    
    private fun checkOfflineMapAvailability() {
        viewModelScope.launch(Dispatchers.IO) {
            // 检查缓存目录中是否有离线地图
            val cacheDir = getApplication<Application>().cacheDir
            val osmdroidDir = java.io.File(cacheDir, "osmdroid")
            val tilesDir = java.io.File(osmdroidDir, "tiles")
            
            val hasOfflineMaps = tilesDir.exists() && (tilesDir.listFiles()?.isNotEmpty() == true)
            
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(offlineMapAvailable = hasOfflineMaps) }
            }
        }
    }
    
    fun startLocation() {
        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        _uiState.update { 
                            it.copy(
                                currentLocation = GeoPoint(location.latitude, location.longitude),
                                isLocationEnabled = true
                            )
                        }
                    }
                }
            }
            
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
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
    
    fun stopLocation() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        _uiState.update { it.copy(isLocationEnabled = false) }
    }
    
    fun setMapLayer(layer: MapLayerType) {
        _uiState.update { it.copy(currentLayer = layer) }
    }
    
    fun updateCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLon = lon) }
    }
    
    fun updateZoom(zoom: Double) {
        _uiState.update { it.copy(zoomLevel = zoom) }
    }
    
    // 下载离线地图区域
    fun downloadOfflineRegion(region: OfflineRegion) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f) }
            
            // 模拟下载进度（实际应该使用TileDownloader）
            for (i in 1..100) {
                kotlinx.coroutines.delay(50)
                _uiState.update { it.copy(downloadProgress = i / 100f) }
            }
            
            // 下载完成后添加到已下载列表
            _uiState.update { 
                it.copy(
                    isDownloading = false,
                    offlineRegions = it.offlineRegions + region.copy(downloaded = true),
                    offlineMapAvailable = true
                )
            }
        }
    }
    
    // 预置的热门钓场/水库区域
    fun getPresetRegions(): List<OfflineRegion> {
        return listOf(
            OfflineRegion(
                id = "yangtze",
                name = "长江流域",
                center = GeoPoint(30.0, 120.0),
                zoomMin = 5,
                zoomMax = 15,
                size = 500_000_000L
            ),
            OfflineRegion(
                id = "yellow_river",
                name = "黄河流域",
                center = GeoPoint(35.0, 110.0),
                zoomMin = 5,
                zoomMax = 15,
                size = 450_000_000L
            ),
            OfflineRegion(
                id = "dongting",
                name = "洞庭湖区域",
                center = GeoPoint(29.0, 112.0),
                zoomMin = 8,
                zoomMax = 15,
                size = 200_000_000L
            ),
            OfflineRegion(
                id = "poyang",
                name = "鄱阳湖区域",
                center = GeoPoint(29.0, 116.0),
                zoomMin = 8,
                zoomMax = 15,
                size = 180_000_000L
            ),
            OfflineRegion(
                id = "sanyang",
                name = "三峡水库",
                center = GeoPoint(31.0, 110.0),
                zoomMin = 8,
                zoomMax = 15,
                size = 150_000_000L
            ),
            OfflineRegion(
                id = "liaohe",
                name = "辽河流域",
                center = GeoPoint(42.0, 123.0),
                zoomMin = 5,
                zoomMax = 15,
                size = 300_000_000L
            ),
            OfflineRegion(
                id = "heilongjiang",
                name = "黑龙江流域",
                center = GeoPoint(50.0, 127.0),
                zoomMin = 5,
                zoomMax = 15,
                size = 400_000_000L
            ),
            OfflineRegion(
                id = "pearl_river",
                name = "珠江流域",
                center = GeoPoint(23.0, 113.0),
                zoomMin = 5,
                zoomMax = 15,
                size = 350_000_000L
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
}
