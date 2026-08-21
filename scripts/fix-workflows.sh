#!/usr/bin/env bash
#
# fix-workflows.sh — repariert die GitHub-Actions-Workflows.
# ============================================================================
# Warum ein Script und kein Commit? Workflow-Dateien dürfen nur mit einem Token
# mit `workflows`-Berechtigung gepusht werden. Der Agent-Token hat die nicht
# (Push → "refusing to allow a GitHub App to update .github/workflows/...").
# Also: hier anwenden, mit deinen Credentials committen.
#
#   bash scripts/fix-workflows.sh          # Änderungen schreiben
#   bash scripts/fix-workflows.sh --check  # nur prüfen (Exit 1 = Defekt offen)
#   git add .github/workflows && git commit -m "ci: Workflows reparieren"
#
# Behobene Defekte (Details: docs/CI-REPARATUR.md)
#   1  ci.yml            ungültiger Permissions-Scope `artifacts` → startup_failure
#   2  ci.yml            Keystore-Decode ohne Secret erzeugt Müll-Datei
#   3  ci.yml            Release-Artefakt heißt anders als im README
#   4  build-release.yml gequoteter Glob in `ls '…/*.apk'` findet nie eine APK
#   5  build-release.yml fehlendes `$` in `{{ github.repository }}`
#   6  build-release.yml Asset-Name != README (`secureguard-pro-<tag>.apk`)
#   7  build-release.yml unnötiger `pages: write`-Scope
#   8  codeql.yml        `languages: kotlin` existiert nicht → `java-kotlin`
#   9  badge.yml         Badge-URLs zeigen auf das falsche Repo (Dinge88)
#  10  build-docker.yml  Release-Upload ohne `contents: write`
# ============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WF="${REPO_ROOT}/.github/workflows"
MODE="${1:-apply}"

python3 - "$WF" "$MODE" <<'PY'
import sys, pathlib

wf, mode = pathlib.Path(sys.argv[1]), sys.argv[2]
check_only = mode == "--check"
GREEN, RED, YELL, OFF = "\033[1;32m", "\033[1;31m", "\033[1;33m", "\033[0m"

fixes = []          # (datei, beschreibung, alt, neu)

def fix(name, desc, old, new, all_occurrences=False):
    fixes.append((name, desc, old, new, all_occurrences))

# ── 1/2/3 · ci.yml ──────────────────────────────────────────────────────────
fix("ci.yml", "ungültiger Permissions-Scope `artifacts` (macht die Datei ungültig)",
    "  artifacts: write                         # Artefakte hochladen\n",
    "")

fix("ci.yml", "Keystore-Decode ohne gesetztes Secret erzeugt eine Müll-Datei",
    """      - name: Decode Keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: echo "$KEYSTORE_BASE64" | base64 -d > app/secureguard-keystore.jks
""",
    """      - name: Decode Keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          if [ -n "$KEYSTORE_BASE64" ]; then
            echo "$KEYSTORE_BASE64" | base64 -d > app/secureguard-keystore.jks
            echo "Keystore dekodiert — Release wird signiert."
          else
            echo "::notice::Kein KEYSTORE_BASE64 — Release-Build bleibt unsigniert."
          fi
""")

fix("ci.yml", "Release-Artefakt heißt `wischiwaschi-pro.apk` statt `secureguard-pro.apk` (README)",
    """          name: wischiwaschi-pro.apk
          path: ${{ steps.find-apk.outputs.apk_path }}""",
    """          name: secureguard-pro.apk
          path: ${{ steps.find-apk.outputs.apk_path }}""")

# ── 4/5/6/7 · build-release.yml ─────────────────────────────────────────────
fix("build-release.yml", "gequoteter Glob: `ls '…/*.apk'` expandiert nicht → apk_path immer leer",
    """          APK=$(ls 'app/build/outputs/apk/release/*.apk' 2>/dev/null | head -1)
          echo "apk_path=$APK" >> "$GITHUB_OUTPUT"
          echo "apk_name=$(basename "$APK")" >> "$GITHUB_OUTPUT\"""",
    """          APK=$(ls app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)
          if [ -z "$APK" ]; then
            echo "::error::Keine Release-APK unter app/build/outputs/apk/release/"
            exit 1
          fi
          TARGET="app/build/outputs/apk/release/secureguard-pro-${GITHUB_REF_NAME}.apk"
          [ "$APK" = "$TARGET" ] || mv "$APK" "$TARGET"
          echo "apk_path=$TARGET" >> "$GITHUB_OUTPUT"
          echo "apk_name=$(basename "$TARGET")" >> "$GITHUB_OUTPUT\"""")

fix("build-release.yml", "fehlendes `$` in `{{ github.repository }}`",
    "Bitte via GitHub Issues in `{{ github.repository }}`.",
    "Bitte via GitHub Issues in `${{ github.repository }}`.")

fix("build-release.yml", "Artefakt-/Asset-Name != README (`secureguard-pro-<tag>.apk`)",
    "          name: wischiwaschi-pro-${{ github.ref_name }}.apk",
    "          name: secureguard-pro-${{ github.ref_name }}.apk")

fix("build-release.yml", "unnötiger `pages: write`-Scope (Least Privilege)",
    "  contents: write                            # Tag + Release erstellen\n  pages: write\n  packages: read",
    "  contents: write                            # Tag + Release erstellen\n  packages: read")

fix("build-release.yml", "Release-Body nennt den alten APK-Namen",
    "            - **APK**: `wischiwaschi-pro-${{ github.ref_name }}.apk`",
    "            - **APK**: `secureguard-pro-${{ github.ref_name }}.apk`")

fix("build-release.yml", "adb-Beispiel nennt den alten APK-Namen",
    "            adb install -r wischiwaschi-pro-${{ github.ref_name }}.apk",
    "            adb install -r secureguard-pro-${{ github.ref_name }}.apk")

# ── 8 · codeql.yml ──────────────────────────────────────────────────────────
fix("codeql.yml", "CodeQL kennt keine Sprache `kotlin` — korrekt ist `java-kotlin`",
    "          languages: kotlin",
    "          languages: java-kotlin")

# ── 9 · badge.yml ───────────────────────────────────────────────────────────
fix("badge.yml", "Badge-URLs zeigen auf das fremde Repo `Dinge88`",
    "dang88bang-pixel/Dinge88",
    "dang88bang-pixel/wischiwaschi-public", all_occurrences=True)

# ── 10 · build-docker.yml ───────────────────────────────────────────────────
fix("build-docker.yml", "Release-Upload ohne `contents: write` (Default-Token ist read-only)",
    """on:
  workflow_dispatch:
  push:
    branches: [ main, develop ]
    tags: [ 'v*' ]
""",
    """on:
  workflow_dispatch:
  push:
    branches: [ main, develop ]
    tags: [ 'v*' ]

permissions:
  contents: write                            # Release-Assets anhängen
""")

fix("build-docker.yml", "Artefakt-Name != README",
    "          name: wischiwaschi-pro.apk",
    "          name: secureguard-pro.apk")

# ── Anwenden ────────────────────────────────────────────────────────────────
applied = pending = missing = 0
by_file = {}
for name, desc, old, new, all_occ in fixes:
    p = wf / name
    if not p.exists():
        print(f"{YELL}  ? {name}: Datei fehlt — übersprungen ({desc}){OFF}")
        missing += 1
        continue
    s = by_file.get(name, p.read_text())
    if old in s:
        s = s.replace(old, new) if all_occ else s.replace(old, new, 1)
        by_file[name] = s
        if check_only:
            print(f"{RED}  ✗ {name}: {desc}{OFF}")
            pending += 1
        else:
            print(f"{GREEN}  ✓ {name}: {desc}{OFF}")
            applied += 1
    else:
        by_file.setdefault(name, s)
        print(f"  · {name}: bereits ok — {desc}")

if not check_only:
    for name, content in by_file.items():
        (wf / name).write_text(content)

print()
if check_only:
    print(f"{'offene Defekte: %d' % pending if pending else 'Alle bekannten Workflow-Defekte sind behoben.'}")
    sys.exit(1 if pending else 0)
print(f"{applied} Änderung(en) geschrieben, {missing} Datei(en) fehlten.")
print("Weiter mit:  git add .github/workflows && git commit -m 'ci: Workflows reparieren'")
PY
