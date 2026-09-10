#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPERTIES="$ROOT_DIR/.secrets/keystore.properties"
DEFAULT_KEYSTORE="$ROOT_DIR/.secrets/agent-mobile-release.jks"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required." >&2
  exit 1
fi
if [[ ! -f "$PROPERTIES" || ! -f "$DEFAULT_KEYSTORE" ]]; then
  echo "Create the local release keystore first with scripts/create-release-keystore.sh." >&2
  exit 1
fi

gh auth status >/dev/null
REPOSITORY="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"

property() {
  local key="$1"
  awk -F= -v wanted="$key" '$1 == wanted { sub(/^[^=]*=/, ""); print; exit }' "$PROPERTIES"
}

STORE_FILE="$(property storeFile)"
STORE_PASSWORD="$(property storePassword)"
KEY_ALIAS="$(property keyAlias)"
KEY_PASSWORD="$(property keyPassword)"

if [[ -z "$STORE_FILE" || -z "$STORE_PASSWORD" || -z "$KEY_ALIAS" || -z "$KEY_PASSWORD" ]]; then
  echo "The local keystore.properties file is incomplete." >&2
  exit 1
fi

if [[ "$STORE_FILE" = /* ]]; then
  KEYSTORE="$STORE_FILE"
else
  KEYSTORE="$ROOT_DIR/$STORE_FILE"
fi
[[ -f "$KEYSTORE" ]] || { echo "Keystore not found: $KEYSTORE" >&2; exit 1; }

if base64 --help 2>&1 | grep -q -- '-w'; then
  base64_command=(base64 -w 0 "$KEYSTORE")
else
  base64_command=(base64 "$KEYSTORE")
fi

"${base64_command[@]}" | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPOSITORY"
printf '%s' "$STORE_PASSWORD" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$REPOSITORY"
printf '%s' "$KEY_ALIAS" | gh secret set ANDROID_KEY_ALIAS --repo "$REPOSITORY"
printf '%s' "$KEY_PASSWORD" | gh secret set ANDROID_KEY_PASSWORD --repo "$REPOSITORY"

printf 'Configured Android signing secrets for %s.\n' "$REPOSITORY"
