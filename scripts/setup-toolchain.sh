#!/usr/bin/env bash
#
# setup-toolchain.sh — JDK 17 + Android SDK 34 in einem Rutsch.
# ============================================================================
# Erledigt genau das, was `./gradlew assembleDebug` braucht:
#
#   1. Netz-Preflight (welche Hosts sind überhaupt erreichbar?)
#   2. JDK 17 (Temurin) → $JDK_DIR
#   3. Android cmdline-tools + platforms;android-34/26 + build-tools;34.0.0
#   4. local.properties (sdk.dir) + toolchain.env zum Sourcen
#   5. Abschluss-Check via scripts/check-env.sh
#
# Nutzung:
#   bash scripts/setup-toolchain.sh              # alles
#   bash scripts/setup-toolchain.sh --jdk-only
#   bash scripts/setup-toolchain.sh --sdk-only
#   bash scripts/setup-toolchain.sh --check      # nur Preflight + Report
#
# Danach:
#   source toolchain.env && ./gradlew assembleDebug
#
# Konfiguration über ENV:
#   JDK_DIR (default ~/.local/jdk-17)   ANDROID_HOME (default ~/Android/Sdk)
#   ANDROID_API_LEVEL=34  MIN_API_LEVEL=26  ANDROID_BUILD_TOOLS=34.0.0
# ============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

JDK_VERSION="${JDK_VERSION:-17.0.11+9}"
JDK_DIR="${JDK_DIR:-${HOME}/.local/jdk-17}"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}}"
ANDROID_API_LEVEL="${ANDROID_API_LEVEL:-34}"
MIN_API_LEVEL="${MIN_API_LEVEL:-26}"
ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-34.0.0}"
CMDLINE_TOOLS_VERSION="${CMDLINE_TOOLS_VERSION:-11076708}"

MODE="${1:-all}"

say()  { printf '\033[1;34m[toolchain]\033[0m %s\n' "$*"; }
ok()   { printf '  \033[1;32m✓\033[0m %s\n' "$*"; }
bad()  { printf '  \033[1;31m✗\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[toolchain FATAL]\033[0m %s\n' "$*" >&2; exit 1; }

# ── 1. Netz-Preflight ───────────────────────────────────────────────────────
# Ohne diese Hosts ist ein Android-Build technisch unmöglich. Lieber hier
# klar scheitern als 10 Minuten später mitten im Gradle-Resolve.
REQUIRED_HOSTS=(
  "https://dl.google.com/android/repository/repository2-3.xml|Android SDK (cmdline-tools, platforms, build-tools)"
  "https://dl.google.com/dl/android/maven2/|Google Maven (AGP, AndroidX, Compose)"
  "https://repo.maven.apache.org/maven2/|Maven Central (Kotlin, Hilt, Retrofit, …)"
  "https://services.gradle.org/distributions/|Gradle-Distribution (Wrapper 8.5)"
  "https://api.adoptium.net/v3/info/available_releases|Temurin JDK 17"
)

preflight() {
  local blocked=0
  say "Netz-Preflight"
  for entry in "${REQUIRED_HOSTS[@]}"; do
    local url="${entry%%|*}" label="${entry##*|}"
    if curl -fsS -o /dev/null --max-time 20 "$url" 2>/dev/null; then
      ok "$label"
    else
      bad "$label — nicht erreichbar ($url)"
      blocked=$((blocked+1))
    fi
  done
  if [ "$blocked" -gt 0 ]; then
    echo ""
    say "⚠️  $blocked Quelle(n) blockiert. In abgeschotteten Umgebungen (Firewall,"
    say "    Sandbox, Corporate-Proxy) lässt sich das Projekt lokal nicht bauen."
    say "    Alternativen:"
    say "      · Proxy setzen:  export HTTPS_PROXY=… (auch für Gradle in gradle.properties)"
    say "      · Container:     make docker-build   (Dockerfile bringt JDK 17 + SDK 34 mit)"
    say "      · CI:            Pull Request öffnen → .github/workflows/ci.yml baut vollständig"
  fi
  return "$blocked"
}

# ── 2. JDK 17 ───────────────────────────────────────────────────────────────
install_jdk() {
  if [ -x "${JDK_DIR}/bin/javac" ]; then
    ok "JDK bereits vorhanden: $("${JDK_DIR}/bin/java" -version 2>&1 | head -1)"
    return 0
  fi
  local current
  if command -v javac >/dev/null 2>&1; then
    current="$(javac -version 2>&1 | grep -oE '[0-9]+' | head -1)"
    if [ "$current" = "17" ]; then
      ok "System-JDK 17 vorhanden ($(javac -version 2>&1))"
      JDK_DIR="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
      return 0
    fi
    say "System-JDK ist $current — installiere zusätzlich JDK 17 nach $JDK_DIR"
  fi

  # Wichtig: ein *JDK*, kein JRE — AGP/Kotlin brauchen javac + jdk.compiler.
  local enc="${JDK_VERSION/+/%2B}"
  local file="OpenJDK17U-jdk_x64_linux_hotspot_${JDK_VERSION/+/_}.tar.gz"
  local url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-${enc}/${file}"

  say "Lade Temurin ${JDK_VERSION}"
  mkdir -p "$JDK_DIR" || fail "kann $JDK_DIR nicht anlegen"
  if ! curl -fsSL --retry 3 --retry-delay 5 "$url" | tar -xzf - -C "$JDK_DIR" --strip-components=1; then
    fail "JDK-Download fehlgeschlagen: $url"
  fi
  [ -x "${JDK_DIR}/bin/javac" ] || fail "javac fehlt nach Installation (JRE statt JDK erwischt?)"
  ok "JDK 17: $("${JDK_DIR}/bin/java" -version 2>&1 | head -1)"
}

# ── 3. Android SDK ──────────────────────────────────────────────────────────
install_sdk() {
  export ANDROID_HOME="$SDK_ROOT"
  export ANDROID_SDK_ROOT="$SDK_ROOT"
  export JAVA_HOME="${JDK_DIR}"
  export PATH="${JAVA_HOME}/bin:${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:$PATH"

  mkdir -p "${SDK_ROOT}/cmdline-tools"

  if [ ! -x "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" ]; then
    local zip="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    local url="https://dl.google.com/android/repository/${zip}"
    local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' RETURN
    say "Lade cmdline-tools ${CMDLINE_TOOLS_VERSION}"
    curl -fsSL --retry 3 --retry-delay 5 "$url" -o "${tmp}/ct.zip" || fail "Download fehlgeschlagen: $url"
    unzip -qq "${tmp}/ct.zip" -d "${tmp}/x" || fail "Entpacken fehlgeschlagen"
    rm -rf "${SDK_ROOT}/cmdline-tools/latest"
    mv "${tmp}/x/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
  fi
  ok "cmdline-tools: ${SDK_ROOT}/cmdline-tools/latest"

  say "Akzeptiere Lizenzen"
  yes | "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" --licenses --sdk_root="$SDK_ROOT" >/dev/null 2>&1 || true

  say "Installiere Pakete (api ${ANDROID_API_LEVEL} / min ${MIN_API_LEVEL} / build-tools ${ANDROID_BUILD_TOOLS})"
  "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK_ROOT" \
    "platform-tools" \
    "platforms;android-${ANDROID_API_LEVEL}" \
    "platforms;android-${MIN_API_LEVEL}" \
    "build-tools;${ANDROID_BUILD_TOOLS}" >/dev/null || fail "sdkmanager fehlgeschlagen"

  ok "Android SDK: $SDK_ROOT"
}

# ── 4. local.properties + toolchain.env ─────────────────────────────────────
write_env() {
  local lp="${REPO_ROOT}/local.properties"
  if grep -qs '^sdk.dir=' "$lp" 2>/dev/null; then
    sed -i.bak "s|^sdk.dir=.*|sdk.dir=${SDK_ROOT}|" "$lp" && rm -f "${lp}.bak"
  else
    printf 'sdk.dir=%s\n' "$SDK_ROOT" >> "$lp"
  fi
  ok "local.properties → sdk.dir=${SDK_ROOT}"

  cat > "${REPO_ROOT}/toolchain.env" <<EOF
# generiert von scripts/setup-toolchain.sh — 'source toolchain.env'
export JAVA_HOME="${JDK_DIR}"
export ANDROID_HOME="${SDK_ROOT}"
export ANDROID_SDK_ROOT="${SDK_ROOT}"
export PATH="\${JAVA_HOME}/bin:\${ANDROID_HOME}/cmdline-tools/latest/bin:\${ANDROID_HOME}/platform-tools:\${PATH}"
EOF
  ok "toolchain.env geschrieben — 'source toolchain.env'"
}

# ── Ablauf ──────────────────────────────────────────────────────────────────
case "$MODE" in
  --check)
    preflight
    echo ""
    JAVA_HOME="${JDK_DIR}" ANDROID_HOME="${SDK_ROOT}" bash "${REPO_ROOT}/scripts/check-env.sh" --warn
    exit 0
    ;;
  --jdk-only) install_jdk; write_env ;;
  --sdk-only) install_sdk; write_env ;;
  all|*)
    preflight || say "Preflight meldete Probleme — versuche es trotzdem."
    echo ""
    install_jdk
    echo ""
    install_sdk
    echo ""
    write_env
    ;;
esac

echo ""
JAVA_HOME="${JDK_DIR}" ANDROID_HOME="${SDK_ROOT}" \
  PATH="${JDK_DIR}/bin:${SDK_ROOT}/platform-tools:${PATH}" \
  bash "${REPO_ROOT}/scripts/check-env.sh" --warn

echo ""
say "Fertig. Weiter mit:"
say "    source toolchain.env && ./gradlew assembleDebug"
