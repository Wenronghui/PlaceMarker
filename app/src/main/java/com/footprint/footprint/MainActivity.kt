package com.footprint.footprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footprint.footprint.ui.map.MapScreen
import com.footprint.footprint.ui.map.MapViewModel
import com.footprint.footprint.ui.markers.MarkersScreen
import com.footprint.footprint.ui.theme.PlaceMarkerTheme
import com.footprint.footprint.ui.tracks.TracksScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PlaceMarkerTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Map : Screen("map", "地图", Icons.Default.Map)
    data object Markers : Screen("markers", "标记", Icons.Default.LocationOn)
    data object Tracks : Screen("tracks", "轨迹", Icons.Default.Route)
}

@Composable
fun MainScreen() {
    val viewModel: MapViewModel = viewModel()
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Map) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Map, Screen.Markers, Screen.Tracks).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                Screen.Map -> MapScreen(viewModel = viewModel)
                Screen.Markers -> MarkersScreen(viewModel = viewModel)
                Screen.Tracks -> TracksScreen(viewModel = viewModel)
            }
        }
    }
}
