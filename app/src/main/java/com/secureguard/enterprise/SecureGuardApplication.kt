package com.secureguard.enterprise

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.secureguard.enterprise.util.NotificationConstants
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Penner Kombat Application - Minimal Hilt Setup
 * Behält Hilt für Build-Kompatibilität, aber ohne schwere Services.
 */
@HiltAndroidApp
class SecureGuardApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        android.util.Log.i("PennerKombat", "🩸 PENNER KOMBAT BOOT - Bahnhof Edition v1.0.0")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.CHANNEL_ALERTS,
                getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_alerts_desc)
                enableLights(true)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.CHANNEL_SERVICES,
                getString(R.string.notif_channel_services),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_services_desc)
                setShowBadge(false)
            }
        )
    }
}
