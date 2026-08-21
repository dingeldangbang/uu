#!/usr/bin/env bash
#
# install-android-sdk.sh — Android SDK + cmdline-tools + Lizenz-Accept + Build-Pakete.
# ———————————————————————————————————————————————————————————————
# Setzt: ANDROID_HOME / ANDROID_SDK_ROOT, PATH
# Fügt Pakete hinzu: platforms;android-34, android-26 (für min-sdk),
#                    build-tools;34.0.0, platform-tools, emulator (optional)
#
# Optionen:
#   --with-ndk     zusätzlich NDK r26d installieren (falls natives C/C++ nötig)
#
# Empfehlung: für reproduzierbare Builds CMDLINE_TOOLS_SHA256=… setzen.

set -euo pipefail

# ── Konfiguration ────────────────────────────────────────
ANDROID_API_LEVEL="${ANDROID_API_LEVEL:-34}"
MIN_API_LEVEL="${MIN_API_LEVEL:-26}"
ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-34.0.0}"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}}"
CMDLINE_TOOLS_VERSION="${CMDLINE_TOOLS_VERSION:-11076708}"  # Letzter Linux-Build
DOWNLOAD_HOST="${DOWNLOAD_HOST:-https://dl.google.com/android/repository}"
WITH_NDK=0
[ "${1:-}" = "--with-ndk" ] && WITH_NDK=1
NDK_VERSION="${NDK_VERSION:-26.1.10909125}"

say() { printf '\033[1;34m[install-sdk]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[install-sdk FATAL]\033[0m %s\n' "$*" >&2; exit 1; }

case "$(uname -s)" in
  Linux)  PLATFORM="linux" ;;
  Darwin) PLATFORM="mac" ;;
  MINGW*|CYGWIN*|MSYS*) PLATFORM="windows" ;;
  *) fail "Nicht unterstützte Plattform: $(uname -s)" ;;
esac

say "Verwende Plattform = $PLATFORM • SDK_ROOT = $SDK_ROOT"

# ── Vorbereitung Ordner ──────────────────────────────────
mkdir -p "$SDK_ROOT/cmdline-tools"
mkdir -p "$SDK_ROOT"

ZIP="commandlinetools-${PLATFORM}-${CMDLINE_TOOLS_VERSION}_latest.zip"
URL="${DOWNLOAD_HOST}/${ZIP}"

# ── Portable ZIP wird gesaugt, falls nicht vorhanden ─────
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
ZIP_PATH="${TEMP_DIR}/${ZIP}"
say "Herunterladen… $URL"
if ! curl -fsSL "$URL" -o "$ZIP_PATH"; then
  fail "Download fehlgeschlagen: $URL"
fi

# ── Integritätsprüfung ────────────────────────────────────
# Nur prüfen, wenn ein Hash explizit vorgegeben wurde (CMDLINE_TOOLS_SHA256=…).
# Vorher standen hier hartkodierte Fantasie-Hashes, die nie gepasst haben und
# deren Mismatch nur weggeloggt wurde — das ist keine Prüfung, sondern Theater.
if [[ -n "${CMDLINE_TOOLS_SHA256:-}" ]]; then
  ACTUAL="$(sha256sum "$ZIP_PATH" 2>/dev/null | awk '{print $1}')"
  [[ "$ACTUAL" == "$CMDLINE_TOOLS_SHA256" ]] \
    || fail "SHA256-Mismatch: $ACTUAL != $CMDLINE_TOOLS_SHA256"
  say "SHA256 ✔"
else
  say "Kein CMDLINE_TOOLS_SHA256 gesetzt — Integritätsprüfung übersprungen (TLS-Vertrauen)."
fi

# ── Entpacken ─────────────────────────────────────────────
if [[ ! -d "$SDK_ROOT/cmdline-tools/latest" ]]; then
  say "Entpacke nach $SDK_ROOT/cmdline-tools/latest"
  pushd "$SDK_ROOT/cmdline-tools" > /dev/null
  if ! unzip -qq "$ZIP_PATH" 2>/dev/null; then
    # Fallback für Microsoft-Windows-Werkzeuge
    tar -xf "$ZIP_PATH" 2>/dev/null || fail "Entpacken fehlgeschlagen"
  fi
  rm -rf latest
  mv cmdline-tools latest
  popd > /dev/null
fi

# ── ENV setzen ────────────────────────────────────────────
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$PATH"

cat > /tmp/android-env.sh <<EOF
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="\${ANDROID_HOME}/cmdline-tools/latest/bin:\${ANDROID_HOME}/platform-tools:\$PATH"
EOF
say "Umgebungsvariablen nach /tmp/android-env.sh geschrieben. Sourcen via:"
say "    source /tmp/android-env.sh"

# ── Lizenzen akzeptieren ─────────────────────────────────
say "Akzeptiere SDK-Lizenzen"
yes | "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses --sdk_root="$SDK_ROOT" 2>/dev/null || true

# ── Pakete installieren ───────────────────────────────────
PKG_LIST=(
  "platform-tools"
  "platforms;android-${ANDROID_API_LEVEL}"
  "platforms;android-${MIN_API_LEVEL}"
  "build-tools;${ANDROID_BUILD_TOOLS}"
  "extras;google;m2repository"
  "extras;android;m2repository"
)
if [ "$WITH_NDK" = "1" ]; then
  PKG_LIST+=("ndk;${NDK_VERSION}")
  say "NDK ${NDK_VERSION} wird mit installiert."
fi
say "Installiere: ${PKG_LIST[*]}"
"$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
   --sdk_root="$SDK_ROOT" \
   "${PKG_LIST[@]}" >/dev/null

# ── Finalcheck ───────────────────────────────────────────
if [[ ! -x "$SDK_ROOT/platform-tools/adb" ]]; then
  fail "platform-tools/adb fehlt nach Installation."
fi

say "✔  Android SDK konfiguriert unter $SDK_ROOT"
say "  api=${ANDROID_API_LEVEL}  build-tools=${ANDROID_BUILD_TOOLS}  min=${MIN_API_LEVEL}"
if [ "$WITH_NDK" = "1" ]; then
  say "  ndk=${NDK_VERSION}"
fi
