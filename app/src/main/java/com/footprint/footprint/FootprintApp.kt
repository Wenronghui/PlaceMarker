package com.footprint.footprint

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class FootprintApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure OSMDroid
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
