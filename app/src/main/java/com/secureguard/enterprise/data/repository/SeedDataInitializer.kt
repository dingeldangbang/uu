package com.secureguard.enterprise.data.repository

import android.util.Log
import com.secureguard.enterprise.data.model.Alert
import com.secureguard.enterprise.data.model.AlertSeverity
import com.secureguard.enterprise.data.model.AlertType
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetCategory
import com.secureguard.enterprise.data.model.AssetStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Befüllt beim ersten App-Start Beispieldaten.
 *
 * Warum?
 *   Nach Erstinstallation ist die Room-DB leer. Ohne Seed-Daten
 *   sieht der Pilot-Nutzer ein leeres Dashboard und kann nicht
 *   selbst einschätzen, ob die App aktiv funktioniert.
 *
 *   Mit Seed bekommt er 8 Beispiel-Assets + ein paar Alerts —
 *   passend zum UI-Mockup (Roller, Fahrrad, Schlüssel, Tablet, …).
 *
 * Voraussetzung: `repo.assetCount() == 0` → nur dann ausführen,
 * sonst hat der User schon eigene Daten und wir würden sie überschreiben.
 */
@Singleton
class SeedDataInitializer @Inject constructor(
    private val repo: SecureGuardRepository
) {
    suspend fun seedIfEmpty() {
        if (repo.assetCount() > 0) {
            Log.d(TAG, "skip seed — DB not empty")
            return
        }

        val now = System.currentTimeMillis()

        val samples = listOf(
            Asset(
                id            = "AS-001-roller",
                name          = "Roller #1",
                shortName     = "Roller",
                mac           = "AA:BB:CC:01:01:01",
                category      = AssetCategory.VEHICLE,
                latitude      = 51.22770,
                longitude     = 6.77350,
                location      = "Wohnung Düsseldorf",
                batteryPercent = 78,
                rssi          = -45,
                status        = AssetStatus.ONLINE,
                lastSeen      = now - 2 * 60_000,
                tags          = listOf("favorit", "stadtnah"),
                owner         = "operator",
                externalAllowed = true
            ),
            Asset(
                id            = "AS-002-fahrrad",
                name          = "Fahrrad #2",
                shortName     = "Fahrrad",
                mac           = "AA:BB:CC:02:02:02",
                category      = AssetCategory.VEHICLE,
                latitude      = 51.22410,
                longitude     = 6.77000,
                location      = "Garage Bilk",
                batteryPercent = 12,
                rssi          = -60,
                status        = AssetStatus.MAINTENANCE,
                lastSeen      = now - 40 * 60_000,
                tags          = listOf("wartung"),
                owner         = "operator",
                externalAllowed = false
            ),
            Asset(
                id            = "AS-003-schluessel",
                name          = "Schlüssel #3",
                shortName     = "Schlüssel",
                mac           = "AA:BB:CC:03:03:03",
                category      = AssetCategory.GENERIC,
                latitude      = null,
                longitude     = null,
                location      = "Unbekannt",
                batteryPercent = 4,
                rssi          = -90,
                status        = AssetStatus.OFFLINE,
                lastSeen      = now - 130 * 60_000,
                tags          = listOf("kritisch"),
                owner         = "operator",
                externalAllowed = true
            ),
            Asset(
                id            = "AS-004-tablet",
                name          = "Tablet #4",
                shortName     = "Tablet",
                mac           = "AA:BB:CC:04:04:04",
                category      = AssetCategory.DEVICE,
                latitude      = 51.23000,
                longitude     = 6.77800,
                location      = "Büro Königsallee",
                batteryPercent = 92,
                rssi          = -55,
                status        = AssetStatus.ONLINE,
                lastSeen      = now - 15 * 60_000,
                tags          = listOf("dienstlich"),
                owner         = "operator",
                externalAllowed = false
            ),
            Asset(
                id            = "AS-005-smartphone",
                name          = "Smartphone #5",
                shortName     = "Smartphone",
                mac           = "AA:BB:CC:05:05:05",
                category      = AssetCategory.DEVICE,
                latitude      = 51.22800,
                longitude     = 6.77400,
                location      = "Stadtpark",
                batteryPercent = 67,
                rssi          = -70,
                status        = AssetStatus.SEARCHING,
                lastSeen      = now - 5 * 60_000,
                tags          = listOf("privat"),
                owner         = "operator",
                externalAllowed = true
            ),
            Asset(
                id            = "AS-006-rfid",
                name          = "RFID-Reader",
                shortName     = "RFID",
                mac           = "AA:BB:CC:06:06:06",
                category      = AssetCategory.SENSOR,
                latitude      = 51.22600,
                longitude     = 6.77500,
                location      = "Lager Hafen",
                batteryPercent = 100,
                rssi          = -38,
                status        = AssetStatus.ONLINE,
                lastSeen      = now - 60_000,
                tags          = listOf("infrastruktur"),
                owner         = "operator",
                externalAllowed = false
            ),
            Asset(
                id            = "AS-007-gateway",
                name          = "LoRa-Gateway",
                shortName     = "Gateway",
                mac           = "AA:BB:CC:07:07:07",
                category      = AssetCategory.GATEWAY,
                latitude      = 51.22500,
                longitude     = 6.77300,
                location      = "Stadttor",
                batteryPercent = 100,
                rssi          = -25,
                status        = AssetStatus.ONLINE,
                lastSeen      = now - 30_000,
                tags          = listOf("infrastruktur"),
                owner         = "operator",
                externalAllowed = true
            ),
            Asset(
                id            = "AS-008-person",
                name          = "Person-Tracker",
                shortName     = "Person",
                mac           = "AA:BB:CC:08:08:08",
                category      = AssetCategory.PERSON,
                latitude      = 51.22900,
                longitude     = 6.77600,
                location      = "Heinrich-Heine-Allee",
                batteryPercent = 51,
                rssi          = -65,
                status        = AssetStatus.ONLINE,
                lastSeen      = now - 3 * 60_000,
                tags          = listOf("mitarbeiter"),
                owner         = "operator",
                externalAllowed = true
            )
        )

        samples.forEach { repo.upsertAsset(it) }
        Log.i(TAG, "Seeded ${samples.size} assets")

        // 1 historischer Alert
        repo.insertAlert(
            Alert(
                timestamp = now - 30 * 60_000,
                type = AlertType.MAINTENANCE,
                severity = AlertSeverity.WARNING,
                title = "Akku-Kritisch: Fahrrad #2",
                message = "Akku 12% — Wartung empfohlen",
                assetId = "AS-002-fahrrad"
            )
        )
        repo.insertAlert(
            Alert(
                timestamp = now - 10 * 60_000,
                type = AlertType.SECURITY,
                severity = AlertSeverity.INFO,
                title = "GPS-Lock: Smartphone #5",
                message = "Standortwechsel erkannt (51.22800, 6.77400)",
                assetId = "AS-005-smartphone"
            )
        )

        Log.i(TAG, "Seeded 2 alerts")
    }

    companion object { private const val TAG = "SeedDataInitializer" }
}
