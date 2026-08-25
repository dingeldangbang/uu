# 🛡️ SecureGuard Enterprise

Asset-Tracking- & Sicherheits-App für **Android 11+** (Zielgerät: Honeywell CT45P).
Kotlin · Jetpack Compose · Room · Hilt · WorkManager — Ortung über **11 Kanäle**
(BLE, WiFi, GNSS, LoRa, Mesh, …) mit selbstlernendem Agent und Hardware-Barcode-Scanner.

Design-Referenz: Stitch-Projekt „AccessOps" — Palette **Industrial Precision 2.0**
(`#005EB8` · `#1A1C1E` · `#EE3124` · `#F8F9FA`), Schrift **Inter**.
Details: [`docs/DESIGN-SYSTEM.md`](docs/DESIGN-SYSTEM.md).

![CI](https://github.com/dingeldangbang/uu/actions/workflows/ci.yml/badge.svg?branch=main)
![Release](https://github.com/dingeldangbang/uu/actions/workflows/build-release.yml/badge.svg?branch=main)

---

## 📦 Lokal bauen

```bash
# Toolchain einmalig einrichten (JDK 17 + Android SDK 34 + local.properties):
make toolchain          # bzw. bash scripts/setup-toolchain.sh
source toolchain.env    # JAVA_HOME / ANDROID_HOME / PATH

make doctor             # prüft Toolchain + Erreichbarkeit der Download-Quellen

./gradlew assembleDebug # Debug-APK
```

`make toolchain` lädt Temurin **JDK 17** und die Android **cmdline-tools +
platforms;android-34/26 + build-tools;34.0.0**, akzeptiert die Lizenzen und
schreibt `sdk.dir` nach `local.properties`.

> **Gesperrtes Netz?** Der Build braucht `dl.google.com`, `repo.maven.apache.org` und
> `services.gradle.org`. Sind die geblockt, meldet das `make doctor` sofort.
> Fallbacks: `make docker-build` (Dockerfile bringt die komplette Toolchain mit)
> oder ein Push/PR → CI baut auf GitHub-Runnern.

```bash
# Release (signiert):
KEYSTORE_PASSWORD=... KEY_ALIAS=secureguard KEY_PASSWORD=... \
./gradlew assembleRelease    # → app/build/outputs/apk/release/
```

Das Release-Signing erwartet `app/secureguard-keystore.p12` (PKCS12, liegt **nie** im Repo).

## 🚀 Release-Pipeline (GitHub Actions)

Workflow **„Release"** (`build-release.yml`) — baut die signierte APK und
veröffentlicht sie als GitHub-Release-Asset (inkl. SHA256SUMS).

> **Einmalige Admin-Einrichtung:** GitHub erlaubt das Anlegen von
> `.github/workflows/*` und das Setzen von Actions-Secrets nur Konten mit
> `workflows`-/`secrets`-Berechtigung. Deshalb liegen die Workflows als
> Vorlagen unter [`docs/workflows/`](docs/workflows/) bereit:
>
> ```bash
> bash scripts/install-workflows.sh     # kopiert Vorlagen nach .github/workflows/
> git add .github/workflows && git commit -m "ci: Workflows aktivieren" && git push
>
> bash scripts/set-release-secrets.sh signing/secureguard-keystore.p12
> ```

1. **Keystore erzeugen** (PKCS12, ohne keytool):
   ```bash
   openssl req -x509 -newkey rsa:2048 -nodes -days 10950 \
     -keyout key.pem -out cert.pem \
     -subj "/CN=SecureGuard Enterprise/O=SecureGuard/C=DE"
   openssl pkcs12 -export -out secureguard-keystore.p12 \
     -inkey key.pem -in cert.pem -name secureguard \
     -passout pass:<KEIN-SICHERES-PASSWORT>
   base64 -w 0 secureguard-keystore.p12   # → Inhalt für KEYSTORE_BASE64
   ```

2. **Secrets setzen** — entweder per Skript
   `bash scripts/set-release-secrets.sh signing/secureguard-keystore.p12`
   oder manuell (Repo → Settings → Secrets and variables → Actions):

   | Secret | Wert |
   | --- | --- |
   | `KEYSTORE_BASE64` | `base64 -w 0 secureguard-keystore.p12` |
   | `KEYSTORE_PASSWORD` | Keystore-Passwort |
   | `KEY_ALIAS` | `secureguard` |
   | `KEY_PASSWORD` | Key-Passwort |

3. **Tag pushen** (löst den Release-Workflow aus):
   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```
   Alternativ manuell: Actions → Release → **Run workflow**.

4. **Ergebnis:** GitHub-Release mit signierter **`app-release.apk`** + Prüfsumme.

## 🧪 CI-Checks

| Workflow | Wann | Inhalt |
| --- | --- | --- |
| `ci.yml` | Push (main/develop), PR, manuell | Debug-Build → Unit-Tests → Lint → APK-Artefakt |
| `build-release.yml` | Tag `v*`, manuell | Signierte Release-APK + GitHub-Release |

## 🔃 Honeywell DataCollection SDK (CT45P)

Das echte AIDC-SDK wird nur über das Honeywell Tech-Portal als AAR verteilt
(nicht in öffentlichen Maven-Repos). Deshalb kompiliert die App gegen den
**Build-Zeit-Stub `:aidc-stub`**: Auf Nicht-Honeywell-Geräten meldet
`HoneywellScanner.isAvailable()` ehrlich `false`, die App bleibt stabil.

Echtes AAR einbinden: `app/libs/aidc.aar` ablegen und in `app/build.gradle`
`implementation project(':aidc-stub')` durch
`implementation fileTree(dir: 'libs', include: ['*.aar'])` ersetzen —
Details in [`aidc-stub/README.md`](aidc-stub/README.md).

## 📚 Dokumente

- [`docs/ARCHITEKTUR.md`](docs/ARCHITEKTUR.md) — Funktionen, Struktur, Tech-Stack
- [`docs/DESIGN-SYSTEM.md`](docs/DESIGN-SYSTEM.md) — Industrial-Precision-2.0-Palette (Stitch)
- [`docs/SETUP.md`](docs/SETUP.md) — Build-Umgebung (Docker / nativ / manuell)
- [`docs/ANDROID11_COMPAT.md`](docs/ANDROID11_COMPAT.md) — CT45P-/Android-11-Spezifika
- [`docs/BERECHTIGUNGS-AUDIT.md`](docs/BERECHTIGUNGS-AUDIT.md) — Permission-Matrix
- [`BETRIEBSVEREINBARUNG.md`](BETRIEBSVEREINBARUNG.md) — DSGVO/BDSG-Blaupause (Pilot: nicht an UI gebunden)
