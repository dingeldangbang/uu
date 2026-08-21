# 🛡️ wischiwaschi — Honeywell CT45P / Android 11

Vollständige Android-App für Asset-Management + selbstlernender Agent + Honeywell Barcode-Scanner (CT45P-Spezialgerät).

> **Projektname:** wischiwaschi  
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
| 📡 LoRa/LoRaWAN | Stub-Bridge in Room-Command-Queue | (Hardware-SDK nachrüstbar) |
| 🧠 Selbstlernender Agent | ✅ ECHTES ε-greedy Q-Learning | `SecureAgentWorker` + `AgentConfigDao` |
| 🗺️ OpenStreetMap-Karte | ✅ OSMDroid mit echten Marker-Assets | `org.osmdroid:osmdroid-android:6.1.18` |
| 🎮 Fernsteuerung | ✅ 8 Aktionen via PendingCommand → reale Bridge | Room v4 |
| 👁️ Optische Erkennung | ✅ CameraX + ML Kit Object-Detection | `androidx.camera:1.3.1` + `mlkit:object-detection:17.0.1` |
| 🌍 Crowdsourcing | ✅ Retrofit-REST-Client (provider-agnostisch) | Retrofit 2.9.0 |
| 🏙️ Urbane Infrastruktur | ✅ WifiManager + TelephonyManager (echte Scans) | Android SDK |
| 📡 GPS / GLONASS / Galileo | ✅ `FusedLocationProviderClient` LIVE | `play-services-location:21.0.1` |
| 🔒 DSGVO-konform | ✅ Room + AES-Converter | `security-crypto:1.1.0-alpha06` |
| 🔋 Batterie-Observation | ✅ ECHTER `ACTION_BATTERY_CHANGED`-Receiver | `DeviceBatteryProvider` |
| 🔫 **Hardware-Barcode-Scanner (CT45P)** | ✅ Honeywell DataCollection SDK (Build-Zeit-Stub `:aidc-stub`, echtes AAR optional) | `app/libs/` + fileTree |
| 📧 Temporäre E-Mail (provider-agnostisch) | ✅ 4 Provider (FreeCustom, Courier, MailAgent, Apify) | `mcp/EmailProvider` |
| 🛰️ API-Node-Manager | ✅ 11 Knoten (WiGle, DHL, Mosquitto, …) | `agent/ApiNodeManager` |

## 🛠️ Build-Toolchain

| Werkzeug | Version | Quelle |
|----------|---------|--------|
| JDK | 17.0.11 (Temurin) | `scripts/install-java.sh` |
| Gradle | 8.5 (Wrapper, **self-bootstrapping**) | `services.gradle.org` |
| Android cmdline-tools | 11076708 | `dl.google.com` |
| Android platforms | android-34 (compile), android-26 (min) | sdkmanager |
| Build-tools | 34.0.0 | sdkmanager |
| NDK | 26.1 (optional, `--with-ndk`) | sdkmanager |

Alle Werkzeuge werden vom **`Makefile`** koordiniert (`setup`, `bootstrap`, `debug`, `release`, `test`, `lint`, `clean`, `install`). CI-Workflows liegen in `.github/workflows/`.

## 🍯 Honeywell CT45P — was du wissen musst

### Empfohlener Build-Vorgang auf CT45P

1. **Honeywell DataCollection SDK installieren.**
   - **Variante A — Maven:** settings.gradle listet Honeywells Repo.
   - **Variante B — Lokal:** AAR in `app/libs/` + fileTree-Block aktivieren.
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

## 🛡️ Betriebsvereinbarung (Pilot-Blaupause)

`BETRIEBSVEREINBARUNG.md` bleibt **als DSGVO-/BDSG-Blaupause** im Repo,
**aber** im Pilot nicht an die App angebunden. Architektur-TOMs (AES, Coarse-Location, Retention-Timer) sind umgesetzt; Akzeptanz-Dialog folgt produktiv.

## 📋 Schritt-für-Schritt-Anleitung für den Agenten

1. Keystore erzeugen (siehe `docs/SETUP.md`).
2. 4 Secrets in GitHub setzen: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tag pushen: `git tag v1.0.0 && git push origin v1.0.0`.
4. CI baut + Release → APK unter `Releases/tag/v1.0.0`.

## 📂 Verzeichnisstruktur

```
wischiwaschi/
├── .github/workflows/    # 7 CI-Workflows (CI, Release, CodeQL, DepReview, Badges, Docker, Honeywell-experimental)
├── scripts/               # install-java.sh, install-android-sdk.sh, bootstrap.sh, check-env.sh
├── docs/                  # SETUP.md, ANDROID11_COMPAT.md
├── docker/                # (alt)
├── wischiwaschi.md        # ← diese Datei
├── wischiwaschi-delivery.md
├── BETRIEBSVEREINBARUNG.md # Blaupause — Pilot, nicht angebunden
├── Dockerfile + docker-compose.yml + Makefile + env.example
└── app/                   # 79 Kotlin-Dateien
```

## 🛠️ Tech-Stack

- Kotlin 1.9.20, Compose-BOM 2024.02, Material3
- Hilt 2.48 (DI), Room 2.6.1 (DB)
- WorkManager 2.9.0 (Agent)
- FusedLocationProvider 21.0.1 (GPS)
- notification-compat 2.7.0 (Benachrichtigungen)
- **Honeywell AIDC 3.0.0** (Hardware-Scanner)
- osmdroid 6.1.18 (Karte)
- CameraX 1.3.1 + ML Kit (Vision)
- Retrofit 2.9.0 + OkHttp 4.12.0 + Gson
- Paho MQTT 1.2.5 (API-Node-Manager)

**Pilot-Bereitschaft:** ✅ vollständig aktiv auf Honeywell CT45P / Android 11.
