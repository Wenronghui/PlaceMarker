package com.footprint.footprint.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footprint.footprint.data.local.entity.MarkerEntity
import com.footprint.footprint.data.local.entity.TrackPointEntity
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 标记对话框
    var showMarkerDialog by remember { mutableStateOf(false) }
    var markerLat by remember { mutableStateOf(0.0) }
    var markerLon by remember { mutableStateOf(0.0) }
    
    // 标记列表对话框
    var showMarkerListDialog by remember { mutableStateOf(false) }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.startLocation()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.startLocation()
        }
    }
    
    // Apply map layer
    LaunchedEffect(uiState.currentLayer) {
        mapView?.setTileSource(uiState.currentLayer.tileSource)
    }
    
    // Apply location when available
    LaunchedEffect(uiState.currentLocation) {
        mapView?.let { map ->
            uiState.currentLocation?.let { location ->
                if (hasLocationPermission) {
                    map.controller.animateTo(location)
                    map.controller.setZoom(15.0)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            // 搜索栏
            AnimatedVisibility(
                visible = showSearchBar,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 16.sp),
                            placeholder = { Text("搜索地点...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { 
                            showSearchBar = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 搜索按钮
                FloatingActionButton(
                    onClick = { showSearchBar = !showSearchBar },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (showSearchBar) Icons.Default.SearchOff else Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 离线地图按钮
                FloatingActionButton(
                    onClick = { showOfflineDialog = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.OfflinePin,
                        contentDescription = "离线地图",
                        tint = if (uiState.offlineMapAvailable) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 图层选择按钮
                FloatingActionButton(
                    onClick = { showLayerDialog = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "选择图层",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 定位按钮
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            mapView?.let { map ->
                                uiState.currentLocation?.let { location ->
                                    map.controller.animateTo(location)
                                    map.controller.setZoom(17.0)
                                } ?: run {
                                    viewModel.startLocation()
                                }
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = if (uiState.isLocationEnabled) 
                        MaterialTheme.colorScheme.primaryContainer
                    else 
                        MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "定位",
                        tint = if (uiState.isLocationEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 缩放控制
                Column {
                    FloatingActionButton(
                        onClick = { mapView?.controller?.zoomIn() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "放大", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = { mapView?.controller?.zoomOut() },
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "缩小", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map View
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        controller.setZoom(10.0)
                        setTileSource(uiState.currentLayer.tileSource)
                        
                        // Enable tile cache and download
                        setUseDataConnection(true)
                        setMultiTouchControls(true)
                        
                        // Set initial position to China center
                        val initialCenter = GeoPoint(35.0, 105.0)
                        controller.setCenter(initialCenter)
                        
                        // Add scale bar overlay (bottom left)
                        val scaleBarOverlay = ScaleBarOverlay(this).apply {
                            setCentred(true)
                            setScaleBarOffset(150, 10)
                        }
                        overlays.add(scaleBarOverlay)
                        
                        // Add compass (top right area)
                        val compassOverlay = CompassOverlay(
                            ctx,
                            InternalCompassOrientationProvider(ctx),
                            this
                        ).apply {
                            enableCompass()
                        }
                        overlays.add(compassOverlay)
                        
                        // Add my location overlay
                        if (hasLocationPermission) {
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                        }
                        
                        // Add direction indicators (N/S/E/W)
                        val directionOverlay = object : Overlay() {
                            private val textPaint = Paint().apply {
                                textSize = 36f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            
                            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                                if (shadow) return
                                
                                val w = mapView.width.toFloat()
                                val h = mapView.height.toFloat()
                                val cx = w / 2
                                
                                // N - Red, top center
                                textPaint.color = android.graphics.Color.RED
                                textPaint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.WHITE)
                                canvas.drawText("N", cx, 70f, textPaint)
                                
                                // S - Gray, bottom center
                                textPaint.color = android.graphics.Color.GRAY
                                canvas.drawText("S", cx, h - 50f, textPaint)
                                
                                // W - Gray, left center
                                canvas.drawText("W", 40f, h / 2, textPaint)
                                
                                // E - Gray, right center
                                canvas.drawText("E", w - 40f, h / 2, textPaint)
                            }
                        }
                        overlays.add(directionOverlay)
                        
                        // 标记点击监听器
                        val markerClickOverlay = object : Overlay() {
                            override fun onSingleTapConfirmed(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                                e?.let { event ->
                                    val projection = mapView?.projection
                                    val geoPoint = projection?.fromPixels(event.x.toInt(), event.y.toInt()) as? GeoPoint
                                    geoPoint?.let {
                                        markerLat = it.latitude
                                        markerLon = it.longitude
                                        showMarkerDialog = true
                                    }
                                }
                                return true
                            }
                            
                            override fun onLongPress(e: android.view.MotionEvent?, mapView: MapView?): Boolean {
                                e?.let { event ->
                                    val projection = mapView?.projection
                                    val geoPoint = projection?.fromPixels(event.x.toInt(), event.y.toInt()) as? GeoPoint
                                    geoPoint?.let {
                                        markerLat = it.latitude
                                        markerLon = it.longitude
                                        showMarkerDialog = true
                                    }
                                }
                                return true
                            }
                        }
                        overlays.add(markerClickOverlay)
                        
                        // Add map listener for coordinate updates
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                val center = mapCenter as? GeoPoint
                                center?.let {
                                    viewModel.updateCenter(it.latitude, it.longitude)
                                }
                                return false
                            }
                            
                            override fun onZoom(event: ZoomEvent?): Boolean {
                                viewModel.updateZoom(zoomLevelDouble)
                                val center = mapCenter as? GeoPoint
                                center?.let {
                                    viewModel.updateCenter(it.latitude, it.longitude)
                                }
                                return false
                            }
                        })
                        
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    mapView = map
                }
            )
            
            // Coordinates display (bottom left)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(
                        text = "经度: ${String.format("%.6f", uiState.centerLon)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "纬度: ${String.format("%.6f", uiState.centerLat)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "缩放: ${String.format("%.1f", uiState.zoomLevel)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Bottom bar with markers and tracking buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 轨迹记录按钮
                Surface(
                    onClick = {
                        if (uiState.isTracking) {
                            viewModel.stopTracking()
                        } else {
                            viewModel.startTracking()
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = if (uiState.isTracking) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (uiState.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isTracking) "停止记录" else "开始记录",
                            tint = if (uiState.isTracking) 
                                MaterialTheme.colorScheme.onError 
                            else 
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isTracking) "停止" else "记录",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (uiState.isTracking) 
                                MaterialTheme.colorScheme.onError 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                        if (uiState.isTracking) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatDistance(uiState.trackDistance),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                
                // 标记按钮
                Surface(
                    onClick = {
                        mapView?.let { map ->
                            val center = map.mapCenter as? GeoPoint
                            center?.let {
                                markerLat = it.latitude
                                markerLon = it.longitude
                                showMarkerDialog = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AddLocation,
                            contentDescription = "添加标记",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "标记",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                // 查看标记列表按钮
                Surface(
                    onClick = { showMarkerListDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "标记列表",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.markers.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Current layer indicator (top right)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.currentLayer.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            // Location status indicator
            if (uiState.isLocationEnabled) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Recording indicator
            if (uiState.isTracking) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "轨迹记录中 - ${formatDistance(uiState.trackDistance)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
    
    // 添加标记对话框
    if (showMarkerDialog) {
        AddMarkerDialog(
            latitude = markerLat,
            longitude = markerLon,
            onDismiss = { showMarkerDialog = false },
            onConfirm = { title, description, category ->
                viewModel.addMarker(markerLat, markerLon, title, description, category)
                showMarkerDialog = false
            }
        )
    }
    
    // 标记列表对话框
    if (showMarkerListDialog) {
        MarkerListDialog(
            markers = uiState.markers,
            onDismiss = { showMarkerListDialog = false },
            onMarkerClick = { marker ->
                mapView?.controller?.animateTo(GeoPoint(marker.latitude, marker.longitude))
                mapView?.controller?.setZoom(17.0)
                showMarkerListDialog = false
            },
            onDeleteMarker = { marker ->
                viewModel.deleteMarker(marker)
            }
        )
    }
    
    // Layer selection dialog
    if (showLayerDialog) {
        AlertDialog(
            onDismissRequest = { showLayerDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择地图图层")
                }
            },
            text = {
                LazyColumn {
                    items(MapLayerType.entries.toList()) { layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setMapLayer(layer)
                                    showLayerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.currentLayer == layer,
                                onClick = {
                                    viewModel.setMapLayer(layer)
                                    showLayerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = layer.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = layer.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLayerDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
    
    // Offline map dialog
    if (showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OfflinePin, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("离线地图")
                }
            },
            text = {
                Column {
                    if (uiState.isDownloading) {
                        LinearProgressIndicator(
                            progress = { uiState.downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "下载中... ${(uiState.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Text(
                        text = "预置水域/钓场区域",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "下载后可离线使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(viewModel.getPresetRegions()) { region ->
                            OfflineRegionItem(
                                region = region,
                                isDownloaded = uiState.offlineRegions.any { 
                                    it.id == region.id && it.downloaded 
                                },
                                isDownloading = uiState.isDownloading,
                                onDownload = { viewModel.downloadOfflineRegion(region) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOfflineDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun AddMarkerDialog(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("普通") }
    
    val categories = listOf("普通", "钓点", "水库", "湖泊", "河流", "景点", "营地", "停车场")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加标记")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "位置: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Text("类别", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description, category) },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun MarkerListDialog(
    markers: List<MarkerEntity>,
    onDismiss: () -> Unit,
    onMarkerClick: (MarkerEntity) -> Unit,
    onDeleteMarker: (MarkerEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("我的标记 (${markers.size})")
            }
        },
        text = {
            if (markers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "暂无标记",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(markers) { marker ->
                        MarkerListItem(
                            marker = marker,
                            onClick = { onMarkerClick(marker) },
                            onDelete = { onDeleteMarker(marker) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun MarkerListItem(
    marker: MarkerEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = marker.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = marker.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun OfflineRegionItem(
    region: OfflineRegion,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = region.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatFileSize(region.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isDownloaded) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "已下载",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        } else {
            IconButton(
                onClick = onDownload,
                enabled = !isDownloading
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "下载",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format("%.2f km", meters / 1000)
    } else {
        String.format("%.0f m", meters)
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
