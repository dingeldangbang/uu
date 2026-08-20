# 🛡️ wischiwaschi / SecureGuard Enterprise

Asset-Tracking & Sicherheits-App für Android 11+ (Zielgerät: Honeywell CT45P).
84 Kotlin-Dateien, ~9.700 LOC — Compose-UI, Room, Hilt, WorkManager, BLE/WiFi/GNSS/LoRa/Mesh-Suche über **11 Kanäle** (`AgentService.comprehensiveSearchAsset`).

![CI](https://github.com/dang88bang-pixel/wischiwaschi-public/actions/workflows/ci.yml/badge.svg)
![Release](https://github.com/dang88bang-pixel/wischiwaschi-public/actions/workflows/build-release.yml/badge.svg)
![CodeQL](https://github.com/dang88bang-pixel/wischiwaschi-public/actions/workflows/codeql.yml/badge.svg)

## 📦 Lokal bauen

```bash
# JDK 17 + Android SDK 34 (Build-Tools 34.0.0) vorausgesetzt
./gradlew assembleDebug                                  # Debug-APK

# Release (signiert):
KEYSTORE_PASSWORD=... KEY_ALIAS=secureguard KEY_PASSWORD=... \
./gradlew assembleRelease                                # → app/build/outputs/apk/release/
```

Das Release-Signing erwartet `app/secureguard-keystore.jks` (liegt **nie** im Repo).

## 🚀 Release-Pipeline (GitHub Actions)

Workflow **„🚀 MinPro / Release — APK Sign & Publish"** (`build-release.yml`):

1. **Secrets setzen** (Repo → Settings → Secrets and variables → Actions → New repository secret):

   | Secret | Wert |
   | --- | --- |
   | `KEYSTORE_BASE64` | `base64 -w 0 secureguard-keystore.jks` |
   | `KEYSTORE_PASSWORD` | Keystore-Passwort |
   | `KEY_ALIAS` | `secureguard` |
   | `KEY_PASSWORD` | Key-Passwort |

2. **Tag pushen** (löst den Release-Workflow automatisch aus):

   ```bash
   git tag -f v1.0.0 && git push origin v1.0.0 --force
   ```

   Alternativ manuell: Actions → Release-Workflow → **Run workflow** → Branch: `v1.0.0`.

3. **Ergebnis:** GitHub-Release `v1.0.0` mit Asset **`secureguard-pro-v1.0.0.apk`** (~5–8 min).

## 🧪 CI-Checks

| Workflow | Wann | Inhalt |
| --- | --- | --- |
| `ci.yml` | Push (main/develop), PR, manuell | Lint → Unit-Tests → Debug-APK + Release-Check (R8 + Wegwerf-Keystore) |
| `build-release.yml` | Tag `v*`, manuell | Signierte Release-APK + GitHub-Release |
| `build-docker.yml` | Push/Tag | Reproduzierbarer Build im Container (→ `dist/`) |
| `codeql.yml` | Push, PR, wöchentlich | Security-Scan (Kotlin, Build-Tracing) |
| `dependency-review.yml` | PR | Android-12+-Permissions- & Honeywell-Anbindungs-Check |

Bei Fehlern in PRs postet die CI den Log-Schwanz als **PR-Kommentar**.

## 🔃 Honeywell DataCollection SDK (CT45P)

Das echte AIDC-SDK wird nur über das Honeywell Tech-Portal als AAR verteilt
(nicht in öffentlichen Maven-Repos). Deshalb kompiliert die App gegen den
**Build-Zeit-Stub `:aidc-stub`** (`com.honeywell.aidc.*`, `BarcodeReader.from()`
→ `null`): Auf Nicht-Honeywell-Geräten meldet `HoneywellScanner.isAvailable()`
ehrlich `false`, die App bleibt stabil.

Echtes AAR einbinden: `app/libs/aidc.aar` ablegen, in `app/build.gradle`
`implementation project(':aidc-stub')` durch `implementation fileTree(dir: 'libs', include: ['*.aar'])`
ersetzen — Details in [`aidc-stub/README.md`](aidc-stub/README.md) und im
Workflow `honeywell-experimental.yml`.

## 📚 Weitere Dokumente

- [`wischiwaschi.md`](wischiwaschi.md) — Projektdoku
- [`wischiwaschi-delivery.md`](wischiwaschi-delivery.md) — Delivery-Notizen
- [`BETRIEBSVEREINBARUNG.md`](BETRIEBSVEREINBARUNG.md) — Blueprint (nicht an UI gebunden)

<!-- build-verification round -->
