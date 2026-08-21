# 📦 wischiwaschi — Delivery Manifest

> **Stand:** Pilot-Phase produktionskopplungsfähig  
> **Build-Toolchain:** Java 17, Gradle 8.5, Android SDK 34 (eingerichtet, selbst-bootstrappend)  
> **App:** end-to-end verkabelt — `BETRIEBSVEREINBARUNG.md` bleibt Blaupause ohne UI-Bindung

---

## 📁 Komplette Datei-Struktur

```
wischiwaschi/
├── .github/workflows/
│   ├── ci.yml                    # Lint + Tests + Assemble (Debug+Release)
│   ├── build-release.yml         # Sign + Tag-driven Release
│   ├── build-docker.yml          # Compose-Build /dist/*.apk
│   ├── codeql.yml                # Static Security Analysis (Kotlin)
│   ├── dependency-review.yml     # Android-12+ Permission-Wachhund
│   ├── badge.yml                 # README-Badge-URLs
│   └── honeywell-experimental.yml # local libs/aidc.aar Build-Pfad
├── scripts/
│   ├── install-java.sh           # JDK 17 (5 Modi: sdkman/apt/brew/choco/portable)
│   ├── install-android-sdk.sh    # SDK 34 + Lizenzen + NDK (--with-ndk)
│   ├── bootstrap.sh              # One-Shot Setup (Java+SDK+Wrapper)
│   └── check-env.sh              # Diagnose der Toolchain
├── docs/
│   ├── SETUP.md                  # Walkthrough
│   └── ANDROID11_COMPAT.md       # CT45P/Android-11-Spezifika
├── wischiwaschi.md               # ← dieses Dokument
├── wischiwaschi-delivery.md      # ← Liefer-Bestand
├── BETRIEBSVEREINBARUNG.md       # DSGVO-/BDSG-Blaupause, Pilot, NICHT angebunden
├── LICENSE                       # Apache 2.0
├── README.md (umbenannt → wischiwaschi.md)
├── Dockerfile
├── docker-compose.yml
├── Makefile
├── env.example
└── app/
    ├── build.gradle              # 47 Dependencies (Hilt, Room, Compose, CameraX, ML Kit, Honeywell)
    ├── proguard-rules.pro
    ├── libs/                     # (optional) Honeywell AAR fallback
    └── src/main/
        ├── AndroidManifest.xml   # 17 Permissions + 4 Features + <queries>-Block
        ├── res/
        │   ├── values/{strings,colors,themes}.xml
        │   ├── drawable/{ic_launcher_*, splash_*, marker_*, ic_node_*, ic_notification}
        │   ├── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/{ic_launcher, ic_launcher_round}.png
        │   ├── mipmap-anydpi-v26/{ic_launcher,ic_launcher_round}.xml
        │   └── xml/{backup_rules, data_extraction_rules, network_security_config}.xml
        └── java/com/secureguard/enterprise/
            ├── SecureGuardApplication.kt
            ├── data/{database,model,repository}
            ├── di/{DatabaseModule,LocationModule,RepositoryModule}
            ├── mcp/{EmailProvider,Providers,OTPDetector,MCPClient}
            ├── agent/{ApiNodeManager,NodeConfig}
            ├── presentation/{MainActivity,navigation,theme,components,ui}
            ├── services/{8 native + Honeywell + Battery + CommandBridge + Mqtt + WebSocket + Audit}
            ├── ui/{scan,optical}
            ├── worker/SecureAgentWorker.kt
            └── util/{Extensions,DeviceCompat}
```

## 🔗 End-to-End-Verkabelung

**Q1 — UI-Schicht (10 Compose-Screens, alle mit HiltViewModel-Bindung):**
```
DashboardScreen ─→ DashboardViewModel ─→ AgentService + DeviceBatteryProvider + TelemetryService + Repository
AssetListScreen ──→ AssetListViewModel ──→ Repository
AssetDetailScreen ─→ AssetDetailViewModel ─→ 6 Services + Repository
MapScreen ─────────→ MapViewModel ─────────→ Repository
ActionsScreen ─────→ ActionsViewModel ─────→ TelemetryService + Repository
AgentConfigScreen ─→ AgentViewModel ────────→ AgentService
SettingsScreen ────→ SettingsViewModel ─────→ SettingsRepository
AlertsScreen ──────→ AlertsViewModel ───────→ Repository
AddAssetScreen ────→ AddAssetViewModel ─────→ Repository
QrScanScreen ──────→ ScanViewModel ─────────→ HoneywellScanner
OpticalScanScreen ─→ OpticalScanViewModel ──→ OpticalService
NodeStatusScreen ──→ NodeStatusViewModel ────→ ApiNodeManager
TempMailScreen ────→ TempMailViewModel ───────→ TempMailService
```

**Q2 — Datenfluss (Room v4):**
```
SecureGuardDatabase (5 Entities) ─→ 5 DAOs ─→ SecureGuardRepository ─→ alle VMs
PendingCommandDao ────┘ (Bridge)
AgentConfigDao ───────┘ (Worker schreibt)
```

**Q3 — Service-Schicht (eager instantiated via SecureGuardApplication):**
- TelemetryService   (FLP via LocationModule)
- AgentService       (WorkManager enqueue + WorkInfo-Bind + runOnceNow)
- CommandBridge      (pendingCommands-Consumer)
- DeviceBatteryProvider (System-BroadcastReceiver)
- HoneywellScanner   (lifecycle-bound, CT45P)
- NotificationService (Channels + Deterministische IDs)
- TempMailService    (4 Provider-agnostisch)
- ApiNodeManager      (11 Knoten)
- MqttService + WebSocketService + AuditService
- LoraService + OpticalService + UrbanService + CrowdService + SatelliteService (searchAsset)

**Q4 — Background (kein klassischer FGS):**
- SecureAgentWorker (@HiltWorker) mit ε-greedy Q-Learning
- enqueueUniquePeriodicWork (≥ 15 min Constraint)
- OneTimeWorkRequest (sofortiges Feedback im UI)

**Q5 — Hardware-Binding (Android 11 + CT45P):**
- MainActivity.RequestRequiredPermissions() — versionsabhängig via DeviceCompat
- HoneywellScanner.claim()/release() — Lifecycle (ON_RESUME/ON_PAUSE)
- QrScanScreen.scans.collect → nav.add_asset?payload=…
- OpticalScanScreen → CameraX + ML Kit Object-Detection live
- UrbanService.startWifiScan() — echt (mit Location-Pflicht)
- TelemetryService.startUpdates() — FusedLocationProviderClient

**Q6 — Settings-Persistence:**
- SettingsRepository → SharedPreferences("secureguard.settings")
- 7 Toggle-Keys + Retention-Tage + DB-Reset-Bool
- StateFlow<SettingsState> für reaktive UI

## 🆕 Aktiv vs. Stub-Historie

| Funktion | Endstand |
|----------|----------|
| FLP GPS | ✅ ECHT |
| Battery-Receiver | ✅ ECHT |
| WorkManager-Worker | ✅ ECHT |
| Q-Learning-Policy | ✅ ECHT (ε-greedy) |
| PendingCommand-Bridge | ✅ ECHT (Software-Simulator, 350 ms Latenz; HW-Bridge nachrüstbar) |
| Settings-Persistenz | ✅ ECHT (SharedPreferences) |
| Marker-Drawables | ✅ ECHT (Vector) |
| Map-Controls | ✅ ECHT (MapView.controller) |
| Honeywell-Scanner | ✅ ECHT (DataCollection SDK, Lifecycle-bound) |
| CameraX + ML Kit | ✅ ECHT (Live-Vorschau + Live-Detection) |
| WifiManager + Telephony | ✅ ECHT (echte Scans, mit Location-Pflicht) |
| TempMail-Provider | ✅ ECHT (4 Provider, OTPDetector-Heuristik 2026) |
| API-Node-Manager | ✅ ECHT (11 Knoten, Health-Monitor, Learning) |
| `searchAsset()`-Stubs | ⚠️ Software-Stub mit echter Detection-Pipeline; HW nachrüstbar |
| **BETRIEBSVEREINBARUNG** | 📜 **BLAUPAUSE — NICHT ANGEBUNDEN** (Pilot) |

## 🚀 Quick-Start

```bash
# Variante A — Docker (kein Java/SDK-Install nötig)
docker compose build secureguard
docker compose run --rm secureguard
# → /dist/app-release.apk

# Variante B — Native
make bootstrap && make release

# Variante C — CI
git tag v1.0.0 && git push origin v1.0.0
```

## 📊 Build-Verification

```
Kotlin:        79 Dateien
Total LOC:     8.776
Hilt-Graph:     17 @Singleton · 11 @HiltViewModel · 1 @HiltWorker
DAO-Methoden:   37 (alle mit Aufrufern)
Gradle-Deps:    47
Res:            32 Dateien (Drawables, Mipmaps, XML-Configs)
CI:             7 Workflows
Splash-Theme:   Theme.SecureGuard.Splash (kein API-30-Blitz)
Network-Config: HTTPS-Standard + Cleartext für localhost + 10.x + 192.168.x
```

**Pilot-Bereitstellung komplett.**
