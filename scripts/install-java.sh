#!/usr/bin/env bash
#
# install-java.sh — Java 17 (Temurin) eingerichtet.
# ————————————————————————————————————————————————————————————————
# Modi:
#   --sdkman   via SDKMAN (verwendet curl)
#   --apt      Debian/Ubuntu
#   --brew     macOS / Homebrew
#   --choco    Windows via Chocolatey (WSC)
#   --portable Tarball-Layout nach $JAVA_HOME
#
# Standard = Auto-Detect → SDKMAN wenn vorhanden, sonst OS-spezifisch

set -euo pipefail

JAVA_VERSION="${JAVA_VERSION:-17.0.11}"
TEMURIN_DIR="${HOME}/.local/java/${JAVA_VERSION}"

# ── Hilfsmethoden ─────────────────────────────────────────
say() { printf '\033[1;34m[install-java]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[install-java FATAL]\033[0m %s\n' "$*" >&2; exit 1; }

# JDK 17 bereits verfügbar?
if command -v java >/dev/null 2>&1; then
  say "$(java -version 2>&1 | head -1) bereits vorhanden"
  if [[ "$(java -version 2>&1 | head -1 | awk -F'\"' '{print $2}' | cut -d. -f1)" == "17" ]]; then
    say "Java 17 OK — keine Aktion."
    exit 0
  fi
  say "Aber falsche Version — überschreibe durch Installation."
fi

# ── Portables Tarball (Plattform-agnostisch) ─────────────
case "${1:-auto}" in
  --portable)
    openjdk_url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.11_9.tar.gz"
    say "Lade $openjdk_url nach $TEMURIN_DIR"
    mkdir -p "$TEMURIN_DIR"
    curl -fsSL "$openjdk_url" | tar -xzf - -C "$TEMURIN_DIR" --strip-components=1
    echo "JAVA_HOME=$TEMURIN_DIR" >> "${GITHUB_ENV:-/dev/null}"
    export JAVA_HOME="$TEMURIN_DIR"
    export PATH="$JAVA_HOME/bin:$PATH"
    say "java -version:"
    "$JAVA_HOME/bin/java" -version
    ;;

  --sdkman)
    if [[ ! -d "${HOME}/.sdkman" ]]; then
      say "Installiere SDKMAN"
      curl -fsSL "https://get.sdkman.io" | bash
    fi
    # shellcheck source=/dev/null
    source "${HOME}/.sdkman/bin/sdkman-init.sh"
    sdkman_echo=1 sdk install java "${JAVA_VERSION}-tem"
    ;;

  --apt)
    sudo apt-get update
    sudo apt-get install -y openjdk-17-jdk-headless
    ;;

  --brew)
    brew install --cask temurin@"${JAVA_VERSION}"
    ;;

  --choco)
    choco install -y temurin17
    ;;

  auto|*)
    if command -v sdkman-init.sh >/dev/null 2>&1 || [[ -d "${HOME}/.sdkman" ]]; then
      bash "$0" --sdkman
    elif command -v apt >/dev/null 2>&1; then
      bash "$0" --apt
    elif command -v brew >/dev/null 2>&1; then
      bash "$0" --brew
    else
      bash "$0" --portable
    fi
    ;;
esac

# ── Final-Check ─────────────────────────────────────────
if ! command -v java >/dev/null 2>&1; then
  fail "Java konnte nicht installiert werden. Bitte manuell JDK 17 setzen."
fi
say "Java ✔  $(java -version 2>&1 | head -1)"
