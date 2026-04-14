package com.footprint.footprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.footprint.footprint.ui.map.MapScreen
import com.footprint.footprint.ui.theme.PlaceMarkerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PlaceMarkerTheme {
                MapScreen()
            }
        }
    }
}
