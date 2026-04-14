package com.footprint.footprint.ui.markers

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footprint.footprint.domain.model.Marker
import com.footprint.footprint.domain.model.MarkerCategory
import com.footprint.footprint.ui.map.MapViewModel
import com.footprint.footprint.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkersScreen(
    viewModel: MapViewModel = viewModel(),
    onMarkerClick: (Marker) -> Unit = {}
) {
    val markers by viewModel.uiState.collectAsState()
    var markerToDelete by remember { mutableStateOf<Marker?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的标记") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (markers.markers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "暂无标记",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "在地图上长按或点击+按钮添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(markers.markers, key = { it.id }) { marker ->
                    MarkerCard(
                        marker = marker,
                        onClick = { onMarkerClick(marker) },
                        onDelete = {
                            markerToDelete = marker
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }
    
    if (showDeleteDialog && markerToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                markerToDelete = null
            },
            title = { Text("删除标记") },
            text = { Text("确定要删除标记 \"${markerToDelete?.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        markerToDelete?.let { viewModel.deleteMarker(it) }
                        showDeleteDialog = false
                        markerToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        markerToDelete = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MarkerCard(
    marker: Marker,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getMarkerColor(marker.category)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(marker.category),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Marker info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = marker.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (marker.description.isNotBlank()) {
                    Text(
                        text = marker.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Coordinates
                    Text(
                        text = String.format("%.4f, %.4f", marker.latitude, marker.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    // Date
                    Text(
                        text = formatDate(marker.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun getMarkerColor(category: MarkerCategory): Color = when (category) {
    MarkerCategory.CAMP -> MarkerCamp
    MarkerCategory.PEAK -> MarkerPeak
    MarkerCategory.WATER -> MarkerWater
    MarkerCategory.VIEW -> MarkerView
    MarkerCategory.PARKING -> MarkerParking
    MarkerCategory.OTHER -> MarkerOther
}

fun getCategoryEmoji(category: MarkerCategory): String = when (category) {
    MarkerCategory.CAMP -> "🏕️"
    MarkerCategory.PEAK -> "⛰️"
    MarkerCategory.WATER -> "💧"
    MarkerCategory.VIEW -> "👁️"
    MarkerCategory.PARKING -> "🅿️"
    MarkerCategory.OTHER -> "📍"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
