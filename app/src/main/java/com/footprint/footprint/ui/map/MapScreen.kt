package com.footprint.footprint.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
    var isClickToAddMarkerMode by remember { mutableStateOf(false) }
    
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Layer selector button
                FloatingActionButton(
                    onClick = { showLayerDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "选择图层")
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "当前位置")
                }
                
                // Click to add marker mode button
                if (!uiState.isTracking) {
                    FloatingActionButton(
                        onClick = {
                            isClickToAddMarkerMode = !isClickToAddMarkerMode
                        },
                        containerColor = if (isClickToAddMarkerMode) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            if (isClickToAddMarkerMode) Icons.Default.EditLocation else Icons.Default.EditLocationAlt,
                            contentDescription = "点击添加标记"
                        )
                    }
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
                        MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (uiState.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isTracking) "停止记录" else "开始记录"
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
                        
                        // Set initial position to China
                        val initialCenter = GeoPoint(35.0, 105.0)
                        controller.setCenter(initialCenter)
                        centerLat = initialCenter.latitude
                        centerLon = initialCenter.longitude
                        
                        // Add scale bar overlay (bottom left)
                        val scaleBarOverlay = ScaleBarOverlay(this).apply {
                            setCentred(true)
                            setScaleBarOffset(170, 10)
                        }
                        overlays.add(scaleBarOverlay)
                        
                        // Add north arrow overlay (top center)
                        val northArrowOverlay = object : Overlay() {
                            private val paint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 48f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }
                            private val arrowPaint = Paint().apply {
                                color = android.graphics.Color.RED
                                textSize = 56f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }
                            
                            override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
                                if (shadow) return
                                
                                val centerX = mapView.width / 2f
                                val y = 60f
                                
                                // Draw N (red)
                                canvas.drawText("N", centerX, y + 10f, arrowPaint)
                                
                                // Draw other directions (gray)
                                paint.color = android.graphics.Color.GRAY
                                canvas.drawText("S", centerX, mapView.height - 60f, paint)
                                canvas.drawText("W", 40f, mapView.height / 2f, paint)
                                canvas.drawText("E", mapView.width - 40f, mapView.height / 2f, paint)
                            }
                        }
                        overlays.add(northArrowOverlay)
                        
                        // Add compass
                        val compassOverlay = CompassOverlay(
                            ctx,
                            InternalCompassOrientationProvider(ctx),
                            this
                        )
                        compassOverlay.enableCompass()
                        overlays.add(compassOverlay)
                        
                        // Add my location overlay
                        if (hasLocationPermission) {
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                        }
                        
                        // Track touch events to distinguish click from pan
                        var lastTouchX = 0f
                        var lastTouchY = 0f
                        var totalMovedDistance = 0f
                        
                        setOnTouchListener { _, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    lastTouchX = event.x
                                    lastTouchY = event.y
                                    totalMovedDistance = 0f
                                    false
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    val dx = event.x - lastTouchX
                                    val dy = event.y - lastTouchY
                                    totalMovedDistance += kotlin.math.sqrt(dx * dx + dy * dy)
                                    lastTouchX = event.x
                                    lastTouchY = event.y
                                    false
                                }
                                MotionEvent.ACTION_UP -> {
                                    // Only show marker dialog if:
                                    // 1. User is in click-to-add-marker mode AND
                                    // 2. User didn't move the map (less than 20px moved)
                                    if (isClickToAddMarkerMode && totalMovedDistance < 20f) {
                                        val projection = projection
                                        val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as? GeoPoint
                                        if (geoPoint != null) {
                                            pendingMarkerLocation = geoPoint
                                            showAddMarkerDialog = true
                                        }
                                    }
                                    false
                                }
                                else -> false
                            }
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
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "经度: ${String.format("%.6f", centerLon)}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "纬度: ${String.format("%.6f", centerLat)}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Tracking status indicator or click-to-add marker mode indicator
            if (uiState.isTracking || isClickToAddMarkerMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (uiState.isTracking) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.tertiaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isTracking) {
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
                        } else if (isClickToAddMarkerMode) {
                            Icon(
                                Icons.Default.EditLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "点击地图添加标记",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
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
