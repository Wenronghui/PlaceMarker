package com.footprint.footprint.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footprint.footprint.domain.model.Marker
import com.footprint.footprint.domain.model.MarkerCategory
import com.footprint.footprint.domain.model.TrackPoint
import com.footprint.footprint.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

enum class MapLayer(val displayName: String, val tileSource: org.osmdroid.tileprovider.tilesource.ITileSource) {
    STANDARD("标准地图", TileSourceFactory.MAPNIK),
    SATELLITE("卫星地图", TileSourceFactory.USGS_SAT),
    HIKING("徒步地图", TileSourceFactory.OpenTopo),
    CONTOUR("等高线图", TileSourceFactory.OpenTopo)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToMarker: (Marker) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var pendingMarkerLocation by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Track center coordinates for display
    var centerLat by remember { mutableStateOf(0.0) }
    var centerLon by remember { mutableStateOf(0.0) }
    
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
    }
    
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Update map markers when markers list changes
    LaunchedEffect(uiState.markers, uiState.trackPoints) {
        mapView?.let { map ->
            updateMapOverlays(map, uiState)
        }
    }
    
    // Apply map layer
    LaunchedEffect(uiState.currentLayer) {
        mapView?.setTileSource(uiState.currentLayer.tileSource)
    }
    
    // Update center coordinates when map is moved
    LaunchedEffect(mapView) {
        mapView?.let { map ->
            val listener = object : org.osmdroid.events.MapListener {
                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                    val center = map.mapCenter as? GeoPoint
                    center?.let {
                        centerLat = it.latitude
                        centerLon = it.longitude
                    }
                    return false
                }
                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                    val center = map.mapCenter as? GeoPoint
                    center?.let {
                        centerLat = it.latitude
                        centerLon = it.longitude
                    }
                    return false
                }
            }
            map.addMapListener(listener)
        }
    }
    
    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Layer selector button
                FloatingActionButton(
                    onClick = { showLayerDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "选择图层", modifier = Modifier.size(20.dp))
                }
                
                // Current location button
                FloatingActionButton(
                    onClick = {
                        mapView?.let { map ->
                            uiState.currentLocation?.let { location ->
                                map.controller.animateTo(location)
                                map.controller.setZoom(17.0)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "当前位置", modifier = Modifier.size(20.dp))
                }
                
                // Add marker button - 点击此按钮显示标记对话框
                FloatingActionButton(
                    onClick = {
                        // 使用地图中心点作为标记位置
                        val center = mapView?.mapCenter as? GeoPoint
                        if (center != null) {
                            pendingMarkerLocation = center
                            showAddMarkerDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = "添加标记", modifier = Modifier.size(20.dp))
                }
                
                // Start/Stop tracking button
                FloatingActionButton(
                    onClick = {
                        if (uiState.isTracking) {
                            viewModel.stopTracking()
                        } else {
                            viewModel.startTracking()
                        }
                    },
                    containerColor = if (uiState.isTracking) 
                        ComposeColor(0xFFE53935)  // Red for stop
                    else 
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (uiState.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isTracking) "停止记录" else "开始记录",
                        modifier = Modifier.size(24.dp)
                    )
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
                        controller.setZoom(15.0)
                        setTileSource(uiState.currentLayer.tileSource)
                        
                        // Enable tile download
                        setUseDataConnection(true)
                        
                        // Set initial position to China
                        val initialCenter = GeoPoint(35.0, 105.0)
                        controller.setCenter(initialCenter)
                        centerLat = initialCenter.latitude
                        centerLon = initialCenter.longitude
                        
                        // Add scale bar overlay (bottom left)
                        val scaleBarOverlay = ScaleBarOverlay(this).apply {
                            setCentred(true)
                            setScaleBarOffset(150, 10)
                        }
                        overlays.add(scaleBarOverlay)
                        
                        // Add compass
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
                        
                        // Add North arrow overlay (东南西北指示)
                        val northArrowOverlay = object : Overlay() {
                            private val textPaint = Paint().apply {
                                textSize = 40f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            
                            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                                if (shadow) return
                                
                                val centerX = mapView.width / 2f
                                val topY = 80f
                                val bottomY = mapView.height - 40f
                                val leftX = 50f
                                val rightX = mapView.width - 50f
                                val middleY = mapView.height / 2f
                                
                                // Draw N (North) - Red, at top center
                                textPaint.color = android.graphics.Color.RED
                                textPaint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.WHITE)
                                canvas.drawText("N", centerX, topY, textPaint)
                                
                                // Draw S (South) - Gray, at bottom center
                                textPaint.color = android.graphics.Color.GRAY
                                textPaint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.WHITE)
                                canvas.drawText("S", centerX, bottomY, textPaint)
                                
                                // Draw W (West) - Gray, at left center
                                canvas.drawText("W", leftX, middleY, textPaint)
                                
                                // Draw E (East) - Gray, at right center
                                canvas.drawText("E", rightX, middleY, textPaint)
                            }
                        }
                        overlays.add(northArrowOverlay)
                        
                        // Gesture detector for tap and long press
                        val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                // 单击地图：获取点击位置并添加标记
                                val projection = projection
                                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as? GeoPoint
                                if (geoPoint != null) {
                                    pendingMarkerLocation = geoPoint
                                    showAddMarkerDialog = true
                                }
                                return true
                            }
                            
                            override fun onLongPress(e: MotionEvent) {
                                // 长按地图：获取长按位置并添加标记
                                val projection = projection
                                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as? GeoPoint
                                if (geoPoint != null) {
                                    pendingMarkerLocation = geoPoint
                                    showAddMarkerDialog = true
                                }
                            }
                        })
                        
                        // Set touch listener - 滑动地图不会弹出标记框
                        setOnTouchListener { _, event ->
                            gestureDetector.onTouchEvent(event)
                            // 返回false允许地图正常滚动/缩放
                            false
                        }
                        
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
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "经度: ${String.format("%.6f", centerLon)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "纬度: ${String.format("%.6f", centerLat)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }
            
            // Tracking status indicator
            if (uiState.isTracking) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(ComposeColor.Red)
                        )
                        Text(
                            text = "轨迹记录中",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (uiState.currentTrack != null) {
                            Text(
                                text = "• ${String.format("%.1f", uiState.trackDistance / 1000)}km",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Layer selection dialog
            if (showLayerDialog) {
                AlertDialog(
                    onDismissRequest = { showLayerDialog = false },
                    title = { Text("选择地图图层") },
                    text = {
                        Column {
                            MapLayer.entries.forEach { layer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setMapLayer(layer)
                                            showLayerDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = uiState.currentLayer == layer,
                                        onClick = {
                                            viewModel.setMapLayer(layer)
                                            showLayerDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(layer.displayName)
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
        }
    }
    
    // Add Marker Dialog
    if (showAddMarkerDialog && pendingMarkerLocation != null) {
        AddMarkerDialog(
            location = pendingMarkerLocation!!,
            onDismiss = {
                showAddMarkerDialog = false
                pendingMarkerLocation = null
            },
            onConfirm = { name, description, category ->
                viewModel.addMarker(name, description, category, pendingMarkerLocation!!)
                showAddMarkerDialog = false
                pendingMarkerLocation = null
            }
        )
    }
}

private fun updateMapOverlays(map: MapView, uiState: MapUiState) {
    // Remove old markers and polylines (keep location overlay, compass, scale bar, north arrow)
    map.overlays.removeAll { it is OsmMarker || it is Polyline }
    
    // Add markers
    uiState.markers.forEach { marker ->
        val osmMarker = OsmMarker(map).apply {
            position = GeoPoint(marker.latitude, marker.longitude)
            title = marker.name
            snippet = marker.description
            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
        }
        map.overlays.add(osmMarker)
    }
    
    // Add track polyline
    if (uiState.trackPoints.isNotEmpty()) {
        val polyline = Polyline().apply {
            outlinePaint.color = TrackColor.toArgb()
            outlinePaint.strokeWidth = 8f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            setPoints(uiState.trackPoints.map { GeoPoint(it.latitude, it.longitude) })
        }
        map.overlays.add(polyline)
    }
    
    map.invalidate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMarkerDialog(
    location: GeoPoint,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, category: MarkerCategory) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MarkerCategory.OTHER) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加地点标记") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Location info
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "位置: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("标记名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = getCategoryDisplayName(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        MarkerCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(getCategoryDisplayName(category)) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.ifBlank { "未命名标记" }, description, selectedCategory) }
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

fun getCategoryDisplayName(category: MarkerCategory): String = when (category) {
    MarkerCategory.CAMP -> "🏕️ 营地"
    MarkerCategory.PEAK -> "⛰️ 山峰"
    MarkerCategory.WATER -> "💧 水源"
    MarkerCategory.VIEW -> "👁️ 观景点"
    MarkerCategory.PARKING -> "🅿️ 停车点"
    MarkerCategory.OTHER -> "📍 其他"
}
