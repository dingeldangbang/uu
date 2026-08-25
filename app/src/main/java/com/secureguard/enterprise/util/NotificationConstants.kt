package com.secureguard.enterprise.util

/**
 * Zentrale Konstanten für Notification-Channels und -IDs.
 * Wird von [com.secureguard.enterprise.SecureGuardApplication]
 * (Channel-Erzeugung), [com.secureguard.enterprise.services.NotificationService]
 * und [com.secureguard.enterprise.services.AgentService] genutzt.
 */
object NotificationConstants {
    /** Channel für kritische Sicherheitsereignisse (IMPORTANCE_HIGH). */
    const val CHANNEL_ALERTS = "secureguard.alerts"

    /** Channel für Hintergrund-Service-Status (IMPORTANCE_LOW). */
    const val CHANNEL_SERVICES = "secureguard.services"

    /** Basis-ID für Foreground-/Service-Benachrichtigungen. */
    const val FGS_NOTIF_ID = 4100
}
