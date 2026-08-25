#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Release-Signing-Secrets im GitHub-Repo setzen (Repo-Admin)
#
#  Legt die vier Actions-Secrets an, die build-release.yml für
#  die signierte Release-APK benötigt:
#    KEYSTORE_BASE64   — PKCS12-Keystore, base64-kodiert
#    KEYSTORE_PASSWORD — Keystore-Passwort
#    KEY_ALIAS         — Alias im Keystore (Standard: secureguard)
#    KEY_PASSWORD      — Key-Passwort (bei openssl-Export = Keystore-Passwort)
#
#  Nutzung:
#    bash scripts/set-release-secrets.sh signing/secureguard-keystore.p12
#
#  Passwörter werden interaktiv abgefragt (oder via Umgebung:
#  KEYSTORE_PASSWORD / KEY_PASSWORD).
# ─────────────────────────────────────────────────────────────
set -euo pipefail
cd "$(dirname "$0")/.."

P12="${1:-signing/secureguard-keystore.p12}"
if [[ ! -f "$P12" ]]; then
  echo "Keystore nicht gefunden: $P12" >&2
  echo "Hinweis: Erst mit openssl erzeugen (siehe README → Release-Pipeline)." >&2
  exit 1
fi

KEY_ALIAS="${KEY_ALIAS:-secureguard}"

if [[ -z "${KEYSTORE_PASSWORD:-}" ]]; then
  read -r -s -p "KEYSTORE_PASSWORD: " KEYSTORE_PASSWORD; echo
fi
if [[ -z "${KEY_PASSWORD:-}" ]]; then
  read -r -s -p "KEY_PASSWORD (meist = Keystore-Passwort): " KEY_PASSWORD; echo
fi

gh secret set KEYSTORE_BASE64  < <(base64 -w 0 "$P12")
gh secret set KEYSTORE_PASSWORD -b"$KEYSTORE_PASSWORD"
gh secret set KEY_ALIAS         -b"$KEY_ALIAS"
gh secret set KEY_PASSWORD      -b"$KEY_PASSWORD"

echo "✓ Secrets gesetzt: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD"
