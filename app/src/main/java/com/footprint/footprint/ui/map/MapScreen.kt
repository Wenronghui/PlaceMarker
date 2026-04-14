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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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

@OptIn(ExperimentalMaterial3Api::class)
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
                    // Center on location if this is first update
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
        }
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
                    // Download progress
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
                text = formatSize(region.size),
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

fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
