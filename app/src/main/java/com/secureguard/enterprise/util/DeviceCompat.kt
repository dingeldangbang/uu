package com.secureguard.enterprise.util

import android.os.Build

/**
 * Geräte-/Android-Versions-Helfer.
 *
 * Zielgerät: Honeywell CT45P / CT45XP — aktuell Android 11 (API 30).
 * Diese Konstanten machen Version-Gates im Code lesbar und zentral
 * wartbar.
 */
object DeviceCompat {

    /** Gerät läuft auf Android 11 oder höher (API ≥ 30). */
    val isAndroid11Plus: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Android 12+ (API 31+): NEUES BLE-Permission-Modell (BLUETOOTH_SCAN/CONNECT). */
    val isAndroid12Plus: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Android 13+ (API 33+): POST_NOTIFICATIONS + NEARBY_WIFI_DEVICES. */
    val isAndroid13Plus: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Android 14+ (API 34+): FGS-Typ-Pflicht + READ_BASIC_PHONE_STATE. */
    val isAndroid14Plus: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /** Typisches Honeywell-CT45-Gerät? (nur zur Diagnose). */
    val isHoneywellDevice: Boolean
        get() = Build.MANUFACTURER.equals("honeywell", ignoreCase = true)

    /**
     * Kurzer Gerätestatus für Logs / Diagnose.
     * Beispiel: "Honeywell CT45P · Android 11 (API 30) · target 34"
     */
    fun deviceSummary(): String {
        val model = Build.MODEL
        val brand = Build.MANUFACTURER
        return "$brand $model · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}
