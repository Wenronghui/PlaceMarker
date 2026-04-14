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
            
            // 设置离线地图缓存路径
            val basePath = File(cacheDir, "osmdroid")
            osmdroidBasePath = basePath
            osmdroidTileCache = File(basePath, "tiles")
            
            // 允许网络连接下载地图
            osmdroidBasePath?.mkdirs()
            osmdroidTileCache?.mkdirs()
            
            // 缓存设置
            tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100MB
            tileFileSystemCacheTrimBytes = 80L * 1024 * 1024 // 80MB
        }
    }
}
