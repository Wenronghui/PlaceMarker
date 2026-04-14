// This file is deprecated - markers feature removed for pure map app
// Kept for backward compatibility
package com.footprint.footprint.ui.markers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MarkersScreen(
    viewModel: com.footprint.footprint.ui.map.MapViewModel? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("标记功能已移除")
    }
}
