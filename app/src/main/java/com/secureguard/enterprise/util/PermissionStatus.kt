package com.secureguard.enterprise.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Zentrale Auswertung der Runtime-Berechtigungen pro Suchkanal.
 *
 * Ziel: kein Kanal darf stumm sterben. Sowohl der Start-Request in
 * [com.secureguard.enterprise.presentation.MainActivity] als auch das
 * Status-Panel in den Settings ziehen ihre Wahrheit aus dieser Datei.
 *
 * **Nicht enthalten** (bewusst):
 *  · `ACCESS_BACKGROUND_LOCATION` — kein Hintergrund-Tracking.
 *  · Normale Permissions (`ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`,
 *    `READ_BASIC_PHONE_STATE`) — die werden beim Install gewährt, ein
 *    Runtime-Request erzeugt keinen Dialog.
 *  · `CAMERA` — wird kontextbezogen von den Scan-Screens angefragt.
 */
object PermissionStatus {

    /** Ein Feature/Kanal und die Berechtigungen, die er wirklich braucht. */
    data class Channel(
        val label: String,
        val permissions: List<String>,
        /** true = Kanal funktioniert schon mit *einer* der Permissions. */
        val anyOf: Boolean = false
    )

    /** Standort (FINE bevorzugt, COARSE genügt für grobe Ortung). */
    val LOCATION = Channel(
        label = "📡 Standort (GNSS, WLAN-, Zell- und BLE-Ortung)",
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        anyOf = true
    )

    /** BLE-Scan: ab Android 12 eigenes Modell, davor Standort. */
    val BLUETOOTH: Channel
        get() = if (DeviceCompat.isAndroid12Plus) {
            Channel(
                label = "🔵 Bluetooth / BLE-Suche",
                permissions = listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            Channel(
                label = "🔵 Bluetooth / BLE-Suche",
                permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }

    /** WLAN-Scan-Ergebnisse: Location, ab API 33 alternativ NEARBY_WIFI_DEVICES. */
    val WIFI: Channel
        get() = if (DeviceCompat.isAndroid13Plus) {
            Channel(
                label = "📶 WLAN-Umgebungsscan",
                permissions = listOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                anyOf = true
            )
        } else {
            Channel(
                label = "📶 WLAN-Umgebungsscan",
                permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
                anyOf = true
            )
        }

    /**
     * Zell-Scan. `TelephonyManager.getAllCellInfo()` verlangt **Location**,
     * nicht Phone-State — deshalb steht hier auch Location.
     */
    val CELL = Channel(
        label = "🗼 Mobilfunk-Zellscan",
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        anyOf = true
    )

    /** Kamera (optische Erkennung / QR) — kontextbezogen angefragt. */
    val CAMERA = Channel(
        label = "📷 Kamera (QR / optische Erkennung)",
        permissions = listOf(Manifest.permission.CAMERA)
    )

    /** Benachrichtigungen (erst ab Android 13 eine Runtime-Permission). */
    val NOTIFICATIONS: Channel?
        get() = if (DeviceCompat.isAndroid13Plus) {
            Channel(
                label = "🔔 Benachrichtigungen",
                permissions = listOf(Manifest.permission.POST_NOTIFICATIONS)
            )
        } else null

    /** Alle Kanäle für das Status-Panel, versionsabhängig. */
    fun allChannels(): List<Channel> =
        listOfNotNull(LOCATION, BLUETOOTH, WIFI, CELL, CAMERA, NOTIFICATIONS)

    fun isGranted(ctx: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    fun isSatisfied(ctx: Context, channel: Channel): Boolean =
        if (channel.anyOf) channel.permissions.any { isGranted(ctx, it) }
        else channel.permissions.all { isGranted(ctx, it) }

    fun missing(ctx: Context, channel: Channel): List<String> =
        if (isSatisfied(ctx, channel)) emptyList()
        else channel.permissions.filterNot { isGranted(ctx, it) }

    /**
     * Permissions, die beim App-Start (einmalig) angefragt werden:
     * Standort, Nearby-Devices und — ab Android 13 — Notifications.
     *
     * Kamera ist **nicht** dabei (Permission-in-Context in den Scan-Screens),
     * normale Permissions ebenfalls nicht (kein Dialog, immer granted).
     */
    fun startupPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (DeviceCompat.isAndroid12Plus) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (DeviceCompat.isAndroid13Plus) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            // Android 11/12: klassische Phone-State-Berechtigung (dangerous).
            add(Manifest.permission.READ_PHONE_STATE)
        }
    }.distinct()

    /** Kurzform „Berechtigung fehlt"-Text für UI/Logs. */
    fun shortName(permission: String): String =
        permission.substringAfterLast('.')
}
