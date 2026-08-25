#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  Workflows installieren (einmalig, durch einen Repo-Admin)
#
#  Kopiert die CI/Release-Workflow-Vorlagen aus docs/workflows/
#  nach .github/workflows/, damit GitHub Actions sie ausführt.
#  (GitHub erlaubt das Anlegen von Workflow-Dateien nur
#   Konten mit `workflows`-Berechtigung — daher dieser Schritt
#   als expliziter Admin-Handgriff statt eines direkten Pushes.)
#
#  Nutzung:
#    bash scripts/install-workflows.sh
#    git add .github/workflows && git commit -m "ci: Workflows aktivieren" && git push
# ─────────────────────────────────────────────────────────────
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p .github/workflows
cp docs/workflows/ci.yml            .github/workflows/ci.yml
cp docs/workflows/build-release.yml .github/workflows/build-release.yml

echo "✓ Workflows installiert: .github/workflows/{ci.yml,build-release.yml}"
echo ""
echo "Nächste Schritte:"
echo "  git add .github/workflows"
echo "  git commit -m \"ci: Workflows aktivieren\""
echo "  git push"
