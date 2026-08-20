#!/usr/bin/env bash
#
# check-env.sh — Build-Umgebung prüfen (Java 17, Android SDK, Gradle, NDK)
# =====================================================================
# Gibt pro Komponente OK / FEHLT aus und endet mit Exit-Code 1, wenn
# eine Pflicht-Komponente fehlt (ohne --warn).
#
# Nutzung:
#   bash scripts/check-env.sh            # streng (Exit 1 bei Fehler)
#   bash scripts/check-env.sh --warn     # nur Bericht, Exit 0
#
set -uo pipefail

WARN_ONLY=0
[ "${1:-}" = "--warn" ] && WARN_ONLY=1

FAIL=0
ok()   { printf '  \033[1;32m✓\033[0m %s\n' "$*"; }
bad()  { printf '  \033[1;31m✗\033[0m %s\n' "$*"; FAIL=$((FAIL+1)); }
info() { printf '  \033[1;36m·\033[0m %s\n' "$*"; }

echo "── Java (benötigt: 17) ──"
JAVA_MAJOR=""
if command -v java >/dev/null 2>&1; then
  JAVA_MAJOR="$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')"
  [ -z "$JAVA_MAJOR" ] && JAVA_MAJOR="$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1)"
  echo "  java: $(java -version 2>&1 | head -1)"
  if [ "$JAVA_MAJOR" = "17" ]; then ok "JDK 17 OK"
  else bad "JDK 17 erforderlich (gefunden: $JAVA_MAJOR) — bash scripts/install-java.sh"; fi
else
  bad "Java fehlt — bash scripts/install-java.sh"
fi

echo ""
echo "── Android SDK (benötigt: platforms;android-34, build-tools;34.0.0) ──"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}}"
if [ -d "$SDK_ROOT" ]; then
  ok "ANDROID_HOME=$SDK_ROOT"
  [ -d "$SDK_ROOT/platforms/android-34" ] && ok "platforms;android-34" || bad "platforms;android-34 fehlt — bash scripts/install-android-sdk.sh"
  [ -d "$SDK_ROOT/build-tools/34.0.0" ] && ok "build-tools;34.0.0" || bad "build-tools;34.0.0 fehlt"
  [ -d "$SDK_ROOT/platform-tools" ] && ok "platform-tools" || bad "platform-tools fehlt"
  [ -x "$SDK_ROOT/platform-tools/adb" ] && ok "adb verfügbar" || info "adb nicht im PATH"
else
  bad "Android SDK fehlt ($SDK_ROOT) — bash scripts/install-android-sdk.sh"
fi

echo ""
echo "── Gradle Wrapper ──"
if [ -x ./gradlew ]; then ok "./gradlew vorhanden (self-bootstrapping)"
else bad "./gradlew fehlt — lädt bei erstem Lauf Gradle 8.5 selbst"; fi

echo ""
echo "── NDK (optional — Projekt nutzt kein natives C/C++) ──"
if [ -d "$SDK_ROOT/ndk" ] 2>/dev/null; then
  info "NDK vorhanden: $(ls "$SDK_ROOT/ndk" 2>/dev/null | head -1)"
else
  info "NDK nicht installiert (nicht erforderlich; optional via install-android-sdk.sh --with-ndk)"
fi

echo ""
echo "── Docker (optional — reproduzierbarer Build) ──"
if command -v docker >/dev/null 2>&1; then ok "docker: $(docker --version 2>/dev/null | head -1)"
else info "Docker nicht installiert — Alternativen: make / scripts / GitHub Actions"; fi

echo ""
if [ "$FAIL" -gt 0 ]; then
  if [ "$WARN_ONLY" = "1" ]; then
    echo "→ $FAIL Warnung(en) (--warn: fortfahren)"
    exit 0
  else
    echo "→ $FAIL Pflicht-Komponente(n) fehlen. Korrektur:"
    echo "    bash scripts/bootstrap.sh    # Java 17 + Android SDK + Wrapper"
    exit 1
  fi
else
  echo "→ Umgebung vollständig."
  exit 0
fi
