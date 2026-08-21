package com.secureguard.enterprise.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.secureguard.enterprise.R
import com.secureguard.enterprise.data.model.Alert
import com.secureguard.enterprise.data.model.AlertSeverity
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.util.NotificationConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val nm = ctx.getSystemService(NotificationManager::class.java)

    fun postAlert(alert: Alert) {
        if (nm == null) return
        val priority = when (alert.severity) {
            AlertSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
            AlertSeverity.WARNING  -> NotificationCompat.PRIORITY_HIGH
            AlertSeverity.INFO     -> NotificationCompat.PRIORITY_DEFAULT
        }
        val n = NotificationCompat.Builder(ctx, NotificationConstants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setPriority(priority)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            nm.notify(notifId(alert.id), n)
        }
    }

    fun postServiceNotification(id: Int, title: String, text: String): Notification {
        val n = NotificationCompat.Builder(ctx, NotificationConstants.CHANNEL_SERVICES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        nm?.notify(id, n)
        return n
    }

    fun cancel(id: Int) {
        nm?.cancel(id)
    }

    /**
     * Spec: sendFoundNotification(asset, rssi) — Benachrichtigt, dass ein
     * gesuchtes Asset gefunden wurde (z.B. nach erfolgreicher Suche).
     */
    fun sendFoundNotification(asset: Asset, rssi: Int) {
        val title = "🎯 Asset gefunden"
        val body  = "${asset.name} (${asset.shortName}) — RSSI: $rssi dBm"
        val builder = NotificationCompat.Builder(ctx, NotificationConstants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (hasPostNotifPermission()) {
            nm?.notify(deterministicActionNotifId(asset.id, "FOUND"), builder.build())
        }
    }

    /** Spec: sendActionNotification(asset, actionType, success). */
    fun sendActionNotification(asset: Asset, actionType: Any, success: Boolean) {
        val title = if (success) "✅ Aktion erfolgreich" else "❌ Aktion fehlgeschlagen"
        val body  = "${asset.name}: ${actionType.toString()}"
        val builder = NotificationCompat.Builder(ctx, NotificationConstants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(
                if (success) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)

        if (hasPostNotifPermission()) {
            nm?.notify(deterministicActionNotifId(asset.id, actionType), builder.build())
        }
    }

    private fun notifId(alertId: Long): Int =
        NOTIF_ID_PREFIX_ALERT + (alertId.coerceAtLeast(0L).toInt() and 0x7FFF_FFFF)

    private fun deterministicActionNotifId(assetId: String, actionType: Any): Int {
        val key = "${assetId}|${actionType.toString()}"
        val hash = key.hashCode()
        return NOTIF_ID_PREFIX_ACTION + (hash and 0x7FFF_FFFF)
    }

    private fun hasPostNotifPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val NOTIF_ID_PREFIX_ALERT  = 10_000_000
        private const val NOTIF_ID_PREFIX_ACTION = 20_000_000
    }
}
