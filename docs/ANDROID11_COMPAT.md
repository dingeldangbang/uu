# 📱 Geräte-Kompatibilität — Honeywell CT45P / CT45XP (Android 11)

> **Zielgerät:** Honeywell CT45P / CT45XP  
> **Betriebssystem:** Android 11 (API 30)  
> **Build-Konfiguration:** `minSdk 26` · `targetSdk 34` · `compileSdk 34`

Eine App mit `targetSdk 34` läuft problemlos auf Android 11. Android-11-spezifische
Verhaltensweisen werden in diesem Dokument erklärt und sind im Code umgesetzt.

---

## 1. Versions-Spezifische Permissions

| Permission | Android 11 (CT45P) | Android 12+ | Android 13+ |
|------------|--------------------|-------------|-------------|
| `ACCESS_FINE_LOCATION` | ✅ Runtime-Dialog | ✅ | ✅ |
| `ACCESS_COARSE_LOCATION` | ✅ Runtime-Dialog | ✅ | ✅ |
| `CAMERA` | ✅ Runtime-Dialog | ✅ | ✅ |
| `READ_PHONE_STATE` | ✅ (klassisch) | ✅ | ❌ → `READ_BASIC_PHONE_STATE` |
| `BLUETOOTH` + `BLUETOOTH_ADMIN` | ✅ (maxSdkVersion=30) | ❌ ersetzt | ❌ ersetzt |
| `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` | ❌ nicht vorhanden | ✅ (API 31+) | ✅ |
| `POST_NOTIFICATIONS` | ❌ nicht vorhanden | ❌ | ✅ (API 33+) |
| `NEARBY_WIFI_DEVICES` | ❌ nicht vorhanden | ❌ | ✅ (API 33+) |

**Umsetzung:** `MainActivity.RequestRequiredPermissions()` baut die Liste
versionsabhängig via `DeviceCompat.isAndroid11Plus/12Plus/13Plus` auf.

## 2. WLAN-Scan auf Android 11 — Standortpflicht

Ab Android 11 liefert `WifiManager.getScanResults()` **nur mit erteilter
Standortberechtigung** Ergebnisse (und WLAN muss eingeschaltet sein).

→ `UrbanService.hasWifiPermission()` prüft daher jetzt **beides**:
`ACCESS_WIFI_STATE` **und** (FINE- oder COARSE-)Location.

> **Bedienhinweis CT45P:** Falls der Knoten "urban" keine WLAN-Netze findet,
> prüfen: (1) Location-Dialog beim ersten Start bestätigt? (2) WLAN aktiviert?

## 3. BLE auf Android 11

- Android 11 nutzt das **alte** BLE-Modell (`BLUETOOTH`, `BLUETOOTH_ADMIN`
  mit `maxSdkVersion="30"` im Manifest).
- Für BLE-Scan-Ergebnisse ist zusätzlich **Location** nötig (gleiche
  Standortpflicht wie WLAN).
- `TelemetryService.searchAsset()` (BLE-Source) arbeitet daher erst nach
  Location-Grant korrekt.

## 4. Honeywell DataCollection SDK (AIDC)

- Das AIDC SDK läuft nativ auf Android 11 — das ist das Standard-Setup des CT45P.
- `HoneywellScanner.claim()` muss aus `Activity.onResume()` (Compose:
  `Lifecycle.Event.ON_RESUME`) aufgerufen werden, `release()` aus `onPause()`.
  Implementiert in `QrScanScreen` via `DisposableEffect`.
- **Package Visibility (`<queries>`)**: Auf Android 11 Pflicht, damit
  `BarcodeReader.from(context)` den AIDC-Service findet — im Manifest enthalten.

## 5. Kamera (CameraX + ML Kit)

- CameraX unterstützt minSdk 21 → läuft auf Android 11.
- `CAMERA`-Permission wird beim ersten Start abgefragt.
- `OpticalScanScreen` bindet die Kamera im Vordergrund (kein Hintergrund-Zugriff).

## 6. Notification-Channels

- Notification-Channels existieren ab API 26 → auf Android 11 verfügbar.
- `POST_NOTIFICATIONS`-Dialog gibt es erst ab API 33 — auf Android 11 werden
  Notifications **immer** angezeigt (kein Opt-in-Dialog nötig).

## 7. Foreground-Services / WorkManager

- Die App deklariert **keine** klassischen `<service>`-Komponenten — alle
  Hintergrund-Logik läuft über **WorkManager** (`SecureAgentWorker`).
- WorkManager funktioniert auf Android 11 uneingeschränkt.
- **Hinweis:** Auf Android 11 gelten Hintergrund-Startbeschränkungen für
  Foreground-Services — durch den WorkManager-Ansatz umgangen.

## 8. Scoped Storage / Dateien

- Android 11 erzwingt Scoped Storage. Die App nutzt ausschließlich
  **app-interne Speicher** (Room-DB, osmdroid-Cache im App-Verzeichnis)
  → keine Storage-Berechtigung nötig, keine Kompatibilitätsprobleme.

## 9. Netzwerk / Cleartext für lokale Pilot-Infrastruktur

Ab `targetSdk 28+` blockiert Android standardmäßig **Cleartext-HTTP** (nur HTTPS).
Für den Pilotbetrieb mit **privater Infrastruktur** (Meshtastic-IP-Tunnel
`10.115.x.x`, lokaler Mosquitto-MQTT-Broker, LAN-API-Server) ist das
hinderlich — deshalb definiert die App eine **Network Security Config**:

`app/src/main/res/xml/network_security_config.xml`

| Bereich | Cleartext |
|---------|-----------|
| Öffentliche Dienste (Standard) | ❌ nur HTTPS |
| `localhost` / `127.0.0.1` | ✅ erlaubt |
| `10.0.0.0/8` (Meshtastic-Tunnel) | ✅ erlaubt |
| `192.168.0.0/16` (LAN) | ✅ erlaubt |

Damit funktioniert z.B. `tcp://10.115.5.2:1883` für den MQTT-Broker oder
`http://192.168.1.20:8080` für ein lokales Backend — ohne App-Crash
und ohne die öffentlichen Dienste unsicher zu machen.

## 10. Log-Diagnose beim Boot

`SecureGuardApplication` loggt beim Start:

```
boot complete · Honeywell CT45P · Android 11 (API 30) · targetSdk=34 · ...
```

→ Damit ist sofort sichtbar, auf welchem Gerät/API-Level die App läuft.

---

## Quick-Check auf dem Gerät

| Test | Erwartung |
|------|-----------|
| App startet | Dashboard zeigt 8 Seed-Assets |
| Location-Dialog | bestätigen → FLP-Updates, WLAN-Scan, BLE funktionieren |
| Kamera-Dialog | bestätigen → OpticalScanScreen zeigt Live-Preview |
| Hardware-Trigger (QrScanScreen) | Honeywell-Scan → AddAsset mit Payload |
| Abfrageknoten (Settings) | NodeStatusScreen zeigt ONLINE/OFFLINE je nach API-Key |
| TempMail (Settings) | Inbox erstellen + OTP abrufen |
