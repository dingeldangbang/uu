# 🏗️ Architektur — SecureGuard Enterprise

Asset-Tracking- & Sicherheits-App für Android 11+ (Zielgerät: **Honeywell CT45P**).
`minSdk 26` · `targetSdk 34` · `compileSdk 34` · Kotlin 1.9.20 · Compose BOM 2024.02.

Design-Referenz: Stitch-Projekt „AccessOps" — siehe [`DESIGN-SYSTEM.md`](DESIGN-SYSTEM.md).

---

## 📁 Struktur

```
├── .github/workflows/        # CI (verify) + Release (signierte APK → GitHub Release)
├── scripts/                  # install-java.sh, install-android-sdk.sh, bootstrap.sh, check-env.sh
├── docs/                     # SETUP.md, ANDROID11_COMPAT.md, BERECHTIGUNGS-AUDIT.md, …
├── aidc-stub/                # Build-Zeit-Stub für das Honeywell DataCollection SDK
├── app/                      # Anwendung (siehe unten)
├── BETRIEBSVEREINBARUNG.md   # DSGVO/BDSG-Blaupause (Pilot: nicht an UI gebunden)
├── Dockerfile / docker-compose.yml / Makefile / env.example
└── local.properties.template
```

```
app/src/main/java/com/secureguard/enterprise/
├── SecureGuardApplication.kt      # Hilt-Root, WorkManager-Config, Notification-Channels
├── agent/                         # ApiNodeManager (11 Abfrageknoten), NodeConfig
├── data/
│   ├── database/                  # Room: SecureGuardDatabase + DAOs
│   ├── model/                     # Asset, Alert, Detection, AgentConfig, PendingCommand, …
│   └── repository/                # SecureGuardRepository, SettingsRepository, SeedDataInitializer
├── di/                            # Hilt-Module (Database, Location, Repository)
├── mcp/                           # E-Mail-Provider (FreeCustom, Courier, MailAgent, Apify) + OTP-Detektor
├── presentation/                  # MainActivity, Navigation, Screens, Components, Theme
├── services/                      # 20+ Dienste (BLE, WiFi, LoRa, Mesh, MQTT, WS, Optical, …)
├── ui/                            # QR-/Optical-Scan-Screens
├── util/                          # DeviceCompat, Extensions, PermissionStatus
└── worker/                        # SecureAgentWorker (ε-greedy Q-Learning)
```

## ✨ Funktionen (aktiv implementiert)

| Modul | Status | Hardware/SDK |
|-------|--------|--------------|
| 📡 LoRa/LoRaWAN | Stub-Bridge in Room-Command-Queue | (Hardware-SDK nachrüstbar) |
| 🧠 Selbstlernender Agent | ✅ ε-greedy Q-Learning | `SecureAgentWorker` + `AgentConfigDao` |
| 🗺️ OpenStreetMap-Karte | ✅ OSMDroid mit Marker-Assets | `org.osmdroid:osmdroid-android:6.1.18` |
| 🎮 Fernsteuerung | ✅ 8 Aktionen via PendingCommand → CommandBridge | Room |
| 👁️ Optische Erkennung | ✅ CameraX + ML Kit Object-Detection | CameraX 1.3.1 + ML Kit 17.0.1 |
| 🌍 Crowdsourcing | ✅ Retrofit-REST-Client (provider-agnostisch) | Retrofit 2.9.0 |
| 🏙️ Urbane Infrastruktur | ✅ WifiManager + TelephonyManager (echte Scans) | Android SDK |
| 📡 GPS / GLONASS / Galileo | ✅ `FusedLocationProviderClient` LIVE | play-services-location 21.0.1 |
| 🔒 DSGVO-konform | ✅ Room + AES-Converter | security-crypto 1.1.0-alpha06 |
| 🔋 Batterie-Observation | ✅ `ACTION_BATTERY_CHANGED`-Receiver | `DeviceBatteryProvider` |
| 🔫 Hardware-Barcode-Scanner (CT45P) | ✅ Honeywell DataCollection SDK (Build-Zeit-Stub `:aidc-stub`, echtes AAR optional) | `app/libs/` + fileTree |
| 📧 Temporäre E-Mail | ✅ 4 Provider (FreeCustom, Courier, MailAgent, Apify) | `mcp/EmailProvider` |
| 🛰️ API-Node-Manager | ✅ 11 Knoten (WiGle, DHL, Mosquitto, …) | `agent/ApiNodeManager` |

## 🛠️ Tech-Stack

- Kotlin 1.9.20, Compose-BOM 2024.02.00, Material3
- Hilt 2.48 (DI), Room 2.6.1 (DB), WorkManager 2.9.0 (Agent)
- FusedLocationProvider 21.0.1 (GPS), BLE-KTX 2.6.1
- osmdroid 6.1.18 (Karte), CameraX 1.3.1 + ML Kit (Vision)
- Retrofit 2.9.0 + OkHttp 4.12.0 + Gson/Moshi, Paho MQTT 1.2.5
- Inter-Font via Downloadable Fonts (Google Fonts Provider, SansSerif-Fallback)

## 🍯 Honeywell CT45P — Integration

- **SDK:** Das echte AIDC-SDK wird nur über das Honeywell Tech-Portal als AAR verteilt.
  Die App kompiliert gegen den Stub `:aidc-stub`; auf Nicht-Honeywell-Geräten meldet
  `HoneywellScanner.isAvailable()` ehrlich `false`, die App bleibt stabil.
- **Echtes AAR:** `app/libs/aidc.aar` ablegen und in `app/build.gradle`
  `implementation project(':aidc-stub')` durch `implementation fileTree(dir: 'libs', include: ['*.aar'])`
  ersetzen — Details in [`aidc-stub/README.md`](../aidc-stub/README.md).
- **Scanner-Trigger:** gelber Seitentrigger; App muss im Foreground sein (`QrScanScreen` offen).
- **Installation:** `adb install -r app/build/outputs/apk/release/app-release.apk`
- **Permissions auf CT45P bestätigen:** Benachrichtigungen, Standort, Bluetooth/BLE-Scan.

## 🛡️ Betriebsvereinbarung

`BETRIEBSVEREINBARUNG.md` ist die DSGVO-/BDSG-Blaupause. Die TOMs (Room-AES,
Coarse-Location, Retention-Timer) sind im Code umgesetzt; die formelle Akzeptanz
folgt beim Produktiv-Rollout. Gerätekompatibilität: [`ANDROID11_COMPAT.md`](ANDROID11_COMPAT.md),
Permission-Matrix: [`BERECHTIGUNGS-AUDIT.md`](BERECHTIGUNGS-AUDIT.md).

## 🚀 Release-Pipeline

1. Keystore als PKCS12 erzeugen (siehe [`SETUP.md`](SETUP.md)) — liegt **nie** im Repo.
2. Secrets setzen: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tag pushen: `git tag v1.0.0 && git push origin v1.0.0`.
4. Workflow `build-release.yml` baut die signierte APK und veröffentlicht sie als GitHub-Release-Asset.
