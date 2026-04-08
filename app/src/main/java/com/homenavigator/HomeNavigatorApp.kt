package com.homenavigator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class HomeNavigatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar osmdroid aquí, UNA sola vez, fuera del hilo de UI
        Configuration.getInstance().apply {
            load(this@HomeNavigatorApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            // Limitar el caché de tiles para no consumir demasiada RAM
            tileDownloadThreads = 2
            tileFileSystemCacheMaxBytes = 50L * 1024 * 1024 // 50 MB
        }
    }
}
