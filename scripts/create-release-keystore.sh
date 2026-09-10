#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRETS_DIR="$ROOT_DIR/.secrets"
KEYSTORE="$SECRETS_DIR/agent-mobile-release.jks"
PROPERTIES="$SECRETS_DIR/keystore.properties"
ALIAS="agent-mobile"

if [[ -e "$KEYSTORE" || -e "$PROPERTIES" ]]; then
  echo "A release keystore already exists under $SECRETS_DIR" >&2
  exit 1
fi

mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"
STORE_PASSWORD="$(openssl rand -hex 32)"
KEY_PASSWORD="$(openssl rand -hex 32)"

keytool -genkeypair \
  -keystore "$KEYSTORE" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Agent Mobile, OU=Mobile, O=dingeldangbang, C=DE" \
  -noprompt

cat > "$PROPERTIES" <<EOF
storeFile=.secrets/agent-mobile-release.jks
storePassword=$STORE_PASSWORD
keyAlias=$ALIAS
keyPassword=$KEY_PASSWORD
EOF
chmod 600 "$KEYSTORE" "$PROPERTIES"

echo "Release keystore created at: $KEYSTORE"
echo "Keep this keystore and its password backed up. It is ignored by Git."
keytool -list -v -keystore "$KEYSTORE" -storepass "$STORE_PASSWORD" | grep -E 'Alias name:|SHA256:'
