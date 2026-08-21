# 🛡️ wischiwaschi / SecureGuard Enterprise

Asset-Tracking & Sicherheits-App für Android 11+ (Zielgerät: Honeywell CT45P).
84 Kotlin-Dateien, ~9.700 LOC — Compose-UI, Room, Hilt, WorkManager, BLE/WiFi/GNSS/LoRa/Mesh-Suche über **11 Kanäle** (`AgentService.comprehensiveSearchAsset`).

![CI](https://github.com/dingeldangbang/uu/actions/workflows/ci.yml/badge.svg)
![Release](https://github.com/dingeldangbang/uu/actions/workflows/build-release.yml/badge.svg)
![CodeQL](https://github.com/dingeldangbang/uu/actions/workflows/codeql.yml/badge.svg)

## 📦 Lokal bauen

```bash
# Toolchain einmalig einrichten (JDK 17 + Android SDK 34 + local.properties):
make toolchain          # bzw. bash scripts/setup-toolchain.sh
source toolchain.env    # JAVA_HOME / ANDROID_HOME / PATH

make doctor             # prüft Toolchain + Erreichbarkeit der Download-Quellen

./gradlew assembleDebug                                  # Debug-APK
```

`make toolchain` lädt Temurin **JDK 17** (kein JRE — `javac` wird gebraucht) und die
Android **cmdline-tools + platforms;android-34/26 + build-tools;34.0.0**, akzeptiert die
Lizenzen und schreibt `sdk.dir` nach `local.properties`.

> **Gesperrtes Netz?** Der Build braucht `dl.google.com`, `repo.maven.apache.org` und
> `services.gradle.org`. Sind die geblockt (Sandbox/Corporate-Proxy), meldet das
> `make doctor` sofort. Fallbacks: `make docker-build` (Dockerfile bringt die komplette
> Toolchain mit) oder Pull Request öffnen → CI baut auf GitHub-Runnern.

```bash
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

## 🔧 CI reparieren (einmalig nötig)

Die Workflows enthalten einen ungültigen Permissions-Scope (`artifacts: write`) und
weitere Defekte — dadurch endet **jeder** Actions-Lauf sofort als `startup_failure`,
die Badges oben sind entsprechend nichts wert. Fix:

```bash
bash scripts/fix-workflows.sh     # oder: git apply docs/ci-repair.patch
git add .github/workflows && git commit -m "ci: Workflows reparieren" && git push
```

Details zu allen neun Defekten: [`docs/CI-REPARATUR.md`](docs/CI-REPARATUR.md).

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
