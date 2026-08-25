# Installations- und Build-Anleitung

Diese Anleitung beschreibt, wie du die komplette Build-Umgebung für **Java 17, Gradle 8.5 und Android SDK 34** lokal einrichtest – drei unterschiedliche Wege, je nach Workflow-Vorliebe.

---

## 1. 🐳 Variante A — Docker (schnellster & atomarer Build)

Voraussetzung: Docker ≥ 20.10.

```bash
docker compose build secureguard          # baut das Image (~1,5 GB)
docker compose run --rm secureguard       # baut APK nach /dist/app-release.apk
```

Vorteile: keine Java- / Android-SDK-Installation auf der Host-Maschine, identische Umgebung wie CI.

---

## 2. 💻 Variante B — Lokal nativ (Linux/macOS/Windows)

### 2.1 Java 17 (Temurin)

**Linux**

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk-headless
```

**mac**

```bash
brew install --cask temurin
```

**Windows** — via Chocolatey

```powershell
choco install -y temurin17
```

**SDKMAN** (empfohlen für Multi-Projekte)

```bash
curl -fsSL "https://get.sdkman.io" | bash
source "${HOME}/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.11-tem
```

Verifizieren:

```bash
java -version
# openjdk version "17.0.11" 2024-04-16
```

### 2.2 Android SDK + Build-Tools

Mit dem beigelegten Bootstrap:

```bash
bash scripts/install-android-sdk.sh
```

Standard-Installations-Pfad: `$HOME/Android/Sdk`. Für CI/Custom: `ANDROID_HOME=/pfleger/sdk-root bash scripts/install-android-sdk.sh`.

Was installiert wird:

| Komponente | Version | Zweck |
|------------|---------|-------|
| cmdline-tools | 11076708 | Bootstrap, Lizenz-Verwaltung |
| platform-tools | aktuell | `adb`, `fastboot` |
| platforms;android-34 | API 34 | compileSdk/targetSdk |
| platforms;android-26 | API 26 | minSdk |
| build-tools;34.0.0 | 34.0.0 | aapt2, d8, zipalign, apksigner |
| extras;google;m2repository | – | Google Play-Services AAR-Cache |
| extras;android;m2repository | – | Android-Support AAR-Cache |

### 2.3 Gradle Wrapper

Der Wrapper wird über das GitHub-Actions-Workflow geliefert:** die `gradlew`-Binary liegt im Repo. Falls fehlt:

```bash
# Variante: Docker (kein Java-Install nötig)
docker run --rm -v "$PWD":/src -w /src gradle:8.5 gradle wrapper --gradle-version 8.5 --distribution-type bin

# Variante: Pure curl + unzip
curl -fsSL https://services.gradle.org/distributions/gradle-8.5-bin.zip | tar -xzf - -C "$HOME/.gradle/distrib/"
"$HOME/.gradle/distrib/gradle-8.5/bin/gradle" wrapper --gradle-version 8.5 --distribution-type bin
chmod +x ./gradlew
```

### 2.4 Env-Variablen (für tcsh/bash)

```bash
cat >> ~/.bashrc <<'EOF'
export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(which java)")")")"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"
EOF
source ~/.bashrc
```

---

## 3. 🔧 Variante C — `bootstrap.sh` (one-shot)

```bash
bash scripts/bootstrap.sh             # Auto-Detect OS
# oder
bash scripts/bootstrap.sh --sdkman   # via SDKMAN
bash scripts/bootstrap.sh --apt
bash scripts/bootstrap.sh --brew
bash scripts/bootstrap.sh --portable # Tarball → $JAVA_HOME
```

Test:

```bash
./gradlew --version
./gradlew assembleDebug
```

---

## 4. 🌐 Netzwerk-Anforderungen

| Quelle | Verwendung |
|--------|------------|
| `dl.google.com/android/repository` | Android-SDK-Cmdline-Tools, Build-Tools, Platforms |
| `services.gradle.org` | Gradle-Wrapper-Distribution |
| `repo.maven.apache.org`, `dl.google.com/dl/android/maven2` | Bibliotheks-AAR-Dependencies |
| Honeywell-AIDC-SDK | via Build-Zeit-Stub `:aidc-stub`; echtes AAR optional über Tech-Portal (`app/libs/`) |
| `api.adoptium.net`, `get.sdkman.io` | JDK 17 |
| `get.honeywell.com` portable AAR | Honeywell AAR-Download |

Falls eine Firewall/CDN eingeschränkt ist: HTTP-Proxy in `gradle.properties` setzen:

```properties
systemProp.http.proxyHost=proxy.firma
systemProp.http.proxyPort=3128
systemProp.https.proxyHost=proxy.firma
systemProp.https.proxyPort=3128
```

---

## 5. 🔐 Keystore einrichten (für Release-Builds)

Sobald die CI laufen soll, base64-Keystore in `KEYSTORE_BASE64`-Secret hinterlegen.

Das Release-Signing erwartet `app/secureguard-keystore.p12` (PKCS12) — ohne keytool per `openssl pkcs12 -export` erzeugen:

```bash
openssl req -x509 -newkey rsa:2048 -nodes -days 10950 \
  -keyout key.pem -out cert.pem \
  -subj "/CN=SecureGuard Enterprise/O=SecureGuard/C=DE"
openssl pkcs12 -export -out secureguard-keystore.p12 \
  -inkey key.pem -in cert.pem -name secureguard \
  -passout pass:<EIGENES-PASSWORT>
base64 -w 0 secureguard-keystore.p12 > keystore.b64
# Inhalt in GitHub-Secret: KEYSTORE_BASE64=...
```

Das Passwort wird zusätzlich als `KEYSTORE_PASSWORD`- und `KEY_PASSWORD`-Secret hinterlegt (bei der openssl-Variante sind beide identisch).

---

## 6. ✅ Erste Build-Validation

```bash
# Smoke-Test: kompletter Compile
./gradlew --no-daemon --console=plain clean help

# Toolchain-Validation (welches JDK wird vom Wrapper genutzt?)
./gradlew --no-daemon -q javaToolchains

# APK-Liste nach Build
./gradlew --no-daemon assembleDebug
ls -lh app/build/outputs/apk/debug/
```
