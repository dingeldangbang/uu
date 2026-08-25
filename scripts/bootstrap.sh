#!/usr/bin/env bash
#
# bootstrap.sh — One-Shot Setup für Entwickler-Maschinen.
# ——————————————————————————————————————————————————————————
# installiert (falls fehlt)
#   1) Java 17      → scripts/install-java.sh
#   2) Android SDK  → scripts/install-android-sdk.sh
# bringt danach den Gradle-Wrapper auf und prüft:
#   3) Gradle-Wrapper vorhanden + ./gradlew --version

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

say() { printf '\033[1;34m[bootstrap]\033[0m %s\n' "$*"; }

# OS-Hinweise
say "=====  Systemvoraussetzungen  ====="
say "OS=$(uname -a)"
say ""

# Schritt 1: Java 17
say "Schritt 1/3 — Java installieren"
bash "$ROOT/scripts/install-java.sh" "${1:-auto}"

# Schritt 2: Android SDK
say ""
say "Schritt 2/3 — Android SDK installieren"
bash "$ROOT/scripts/install-android-sdk.sh"

# Schritt 3: Gradle-Wrapper
say ""
say "Schritt 3/3 — Gradle-Wrapper vorbereiten"
if [[ ! -x gradlew ]]; then
  say "gradlew fehlt — einmaliger Bootstrap aus echtem Gradle-Image:"
  if command -v docker >/dev/null 2>&1; then
    docker run --rm -v "$ROOT":/src -w /src gradle:8.5 gradle wrapper --gradle-version 8.5 --distribution-type bin
  else
    say "Boots Gradle ueber SDKMAN-Bridge (Internet):"
    curl -fsSL "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o /tmp/gradle.zip
    mkdir -p "${HOME}/.gradle/distrib"
    unzip -qq /tmp/gradle.zip -d "${HOME}/.gradle/distrib/"
    "${HOME}/.gradle/distrib/gradle-8.5/bin/gradle" wrapper --gradle-version 8.5 --distribution-type bin
  fi
fi

chmod +x gradlew
say ""
say "Ergebnis:"
./gradlew --version | head -10
say ""
say "✔  Bootstrap vollständig. Jetzt möglich:"
say "   ./gradlew assembleDebug       # build test-APK"
say "   ./gradlew assembleRelease     # signed APK"
say "   ./gradlew test                 # Unit-Tests"
