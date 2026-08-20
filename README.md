# 🛡️ SecureGuard Enterprise — Honeywell CT45P / Android 11

Vollständige Android-App für Asset-Management + selbstlernender Agent + Honeywell Barcode-Scanner (CT45P-Spezialgerät).

> **Zielgerät:** Honeywell CT45P / CT45XP — **Android 11 (API 30)**  
> **Build:** `minSdk 26` · `targetSdk 34` · `compileSdk 34`  
> Android-11-spezifische Details: siehe [`docs/ANDROID11_COMPAT.md`](docs/ANDROID11_COMPAT.md)

## 🚀 Quick-Start (drei Wege, identische Build-Pipeline)

```bash
## A) Docker — atomarer Build, kein lokales Java/SDK nötig
docker compose build secureguard && docker compose run --rm secureguard

## B) Native — macOS/Linux/Windows
make bootstrap   # installiert Java 17 + Android SDK 34 + Wrapper
make debug       # baut Debug-APK
make release     # signierte APK

## C) Manuelle Schritte — siehe docs/SETUP.md
```

**Voraussetzungen:** Internet (für Downloads), 6 GB freier Speicherplatz für Android SDK, JDK 17 falls nicht via Bootstrap.

## ✨ Funktionen (aktive, ausführbare Implementierung)

| Modul | Status | Hardware/SDK |
|-------|--------|--------------|
| 📡 LoRa/LoRaWAN | Stub-Bridge in Room-Command-Queue | (nocht kein Hardware-SDK integriert) |
| 🧠 Selbstlernender Agent | ✅ ECHTES ε-greedy Q-Learning | `SecureAgentWorker` + `AgentConfigDao` |
| 🗺️ OpenStreetMap-Karte | ✅ OSMDroid mit echten Marker-Assets | `org.osmdroid:osmdroid-android:6.1.18` |
| 🎮 Fernsteuerung | ✅ 8 Aktionen via PendingCommand → reale Bridge | Room v3 |
| 👁️ Optische Erkennung | Stub | CameraX/ML-Kit (folgt Phase C) |
| 🌍 Crowdsourcing | Stub | Retrofit (folgt Phase C) |
| 🏙️ Urbane Infrastruktur | Stub | WifiManager / Telephony (folgt Phase C) |
| 📡 GPS / GLONASS / Galileo | ✅ `FusedLocationProviderClient` LIVE | `play-services-location:21.0.1` |
| 🔒 DSGVO-konform | ✅ Room + AES-Converter | `security-crypto:1.1.0-alpha06` |
| 🔋 Batterie-Observation | ✅ ECHTER `ACTION_BATTERY_CHANGED`-Receiver | `DeviceBatteryProvider` |
| 🔫 **Hardware-Barcode-Scanner (CT45P)** | ✅ Honeywell DataCollection SDK LIVE | `com.honeywell.aidc:aidc:3.0.0` |

## 🛠️ Build-Toolchain

| Werkzeug | Version | Quelle |
|----------|---------|--------|
| JDK | 17.0.11 (Temurin) | `scripts/install-java.sh` |
| Gradle | 8.5 (Wrapper, **self-bootstrapping**) | `services.gradle.org` |
| Android cmdline-tools | 11076708 | `dl.google.com` |
| Android platforms | android-34 (compile), android-26 (min) | sdkmanager |
| Build-tools | 34.0.0 | sdkmanager |
| NDK | 26.1 (optional, `--with-ndk`) | sdkmanager |

**`gradlew` ist self-bootstrapping** — es lädt die Gradle-8.5-Distribution bei Bedarf selbst
(kein binäres `gradle-wrapper.jar` im Repo nötig). Einmaliger Aufruf genügt:

```bash
./gradlew --version        # lädt Gradle 8.5 automatisch
./gradlew assembleDebug    # baut die Debug-APK
```

Umgebungs-Check:

```bash
bash scripts/check-env.sh              # streng (Exit 1 bei fehlender Pflicht-Komponente)
bash scripts/check-env.sh --warn       # nur Bericht
```

Alle Werkzeuge werden vom **`Makefile`** koordiniert (`setup`, `bootstrap`, `debug`, `release`, `test`, `lint`, `clean`, `install`).

## 🍯 Honeywell CT45P — was du wissen musst

### Empfohlener Build-Vorgang auf CT45P

1. **Honeywell DataCollection SDK installieren.**
   - **Variante A — Maven (Standard):** settings.gradle listet Honeywells Repo. Wenn Maven-Zugang klappt → `com.honeywell.aidc:aidc:3.0.0` ist automatisch verfügbar.
   - **Variante B — Lokal:** Wenn der Hermes-Build keinen Zugriff zum Hive hat, App-SDK in ``app/libs/`` legen und Zeile entkommentieren:
     ```gradle
     // app/build.gradle
     implementation fileTree(include: ['*.aar'], dir: 'libs')
     ```
   - **Variante C — Tech-Portal:** https://honeywell.com/connect-honeywell → DataCollection SDK → AAR herunterladen und in `app/libs/` ablegen.

2. **APK installieren** (verbunden via USB oder WiFi-Debug):
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

3. **Permission auf CT45P bestätigen** (Android 11 Push-Dialog):
   - `Benachrichtigungen zulassen`
   - `Standort` (einmalig beim ersten Öffnen)
   - `Bluetooth` / `BLE-Scan` (in `Settings` der App)

4. **Hardware-Scanner-Button drücken** — der gelbe Seitentrigger am Gerät. Die App muss aktuell im **Foreground** sein (`QrScanScreen` offen) damit der Hardware-Trigger diesen konsumiert.

### Wo sind die Honeywell-Funktionen?

| Funktion | Datei |
|----------|-------|
| Scanner claim/release (Lifecycle) | `services/HoneywellScanner.kt` |
| ScanViewModel-Bridge | `ui/scan/ScanViewModel.kt` |
| UI mit Live-Scan-Status | `ui/scan/QrScanScreen.kt` |
| Package-Visibility <queries> | `AndroidManifest.xml` |
| Honeywell Dependency | `app/build.gradle` |

## 🚀 Build via GitHub Actions

Repo → GitHub-Push → Actions → `secureguard-pro.apk` als Artifact oder Release.

### Secrets einrichten

| Secret | Wert |
|--------|------|
| `KEYSTORE_BASE64` | base64-codierter Keystore |
| `KEYSTORE_PASSWORD` | `password` |
| `KEY_ALIAS` | `secureguard` |
| `KEY_PASSWORD` | `password` |

## 📂 Verzeichnisstruktur

```
app/src/main/java/com/secureguard/enterprise/
├── SecureGuardApplication.kt
├── data/
│   ├── database/         # Room v3 (5 Entities, 5 DAOs)
│   ├── model/            # Asset, Detection, Alert, AgentConfig, PendingCommand
│   └── repository/       # SecureGuardRepository, SettingsRepository
├── di/                   # DatabaseModule, LocationModule
├── presentation/
│   ├── components/       # StatCard, ActionButton, AssetCard
│   ├── navigation/       # SecureGuardNavHost (Routes-Objekt)
│   ├── theme/            # Material3-Theme
│   ├── ui/
│   │   ├── dashboard/
│   │   ├── assets/        # List + Detail
│   │   ├── map/           # OSMDroid
│   │   ├── actions/
│   │   ├── agent/
│   │   ├── settings/      # SharedPreferences-backed
│   │   ├── alerts/        # Open Alerts
│   │   └── addasset/      # Form + ScannedPayload-of
├── services/             # 9x @Singleton (incl. Honeywell)
└── worker/               # SecureAgentWorker

app/libs/                  # ← (optional) Honeywell-AAR wenn nicht via Maven
```

## 🛠️ Tech-Stack

- Kotlin 1.9.20, Compose-BOM 2024.02, Material3
- Hilt 2.48 (DI), Room 2.6.1 (DB)
- WorkManager 2.9.0 (Agent)
- FusedLocationProvider 21.0.1 (GPS)
- notification-compat 2.7.0 (Benachrichtigungen)
- **Honeywell AIDC 3.0.0** (Hardware-Scanner)
- osmdroid 6.1.18 (Karte)

## ⚠️ Bekannte Werte-Grenzen

| Bereich | Constraint |
|---------|------------|
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` | 34 (für Android 14+ kompiliert) |
| `compileSdk` | 34 |
| WorkManager periodicIntervall | ≥ 15 min (AndroidX-Constraint, klemmt kleinere UI-Werte) |
| Honeywell trigger | Nur via Lifecycle: ON_RESUME → claim(), ON_PAUSE → release() |
