# Penner Kombat Workflows

Diese Workflows bauen die fertige APK via GitHub Actions.

## Warum nicht direkt in .github/workflows/?

GitHub App Token (arena-ai-coding-agent) hat keine `workflows` Permission.
Push von `.github/workflows/` wird mit 403 geblockt:

```
refusing to allow a GitHub App to create or update workflow `.github/workflows/ci.yml` without `workflows` permission
```

## Workaround: Manuell hinzufügen

### Option A: Via GitHub Web UI (empfohlen)

1. Gehe zu https://github.com/dingeldangbang/uu/tree/arena/01a031da-uu
2. Erstelle Ordner `.github/workflows/` via "Add file" -> "Create new file"
3. Kopiere Inhalt von `ci.yml` und `build-release.yml` hier rein
4. Commit direkt auf Branch

### Option B: Lokal mit PAT

```bash
git clone https://github.com/dingeldangbang/uu.git
cd uu
git checkout arena/01a031da-uu
mkdir -p .github/workflows
cp docs/penner-kombat-workflows/*.yml .github/workflows/
git add .github/workflows/
git commit -m "ci: add Penner Kombat workflows"
git push origin arena/01a031da-uu
```

Dazu brauchst du ein Personal Access Token mit `workflows` Scope.

### Option C: Patch anwenden

```bash
git apply docs/workflows-hinzufuegen.patch
# oder
bash scripts/fix-workflows.sh
git add .github/workflows && git commit -m "ci: workflows" && git push
```

## Was die Workflows tun

- `ci.yml`: Bei jedem Push/PR baut Debug + Release APK, lädt als Artifacts hoch
- `build-release.yml`: Bei Tag v* oder manuell baut signierte APK + GitHub Release

## Nach dem Push

1. Gehe zu Actions Tab
2. Warte auf grünen Haken bei "Penner Kombat CI Build"
3. Artifacts -> `penner-kombat-release.apk` downloaden
4. Oder: Tag pushen -> Release mit APK

```bash
git tag -f v1.0.0-bahnhof && git push origin v1.0.0-bahnhof --force
```

## Lokaler Build ohne CI

```bash
make toolchain
source toolchain.env
./gradlew assembleDebug
./gradlew assembleRelease
```

Siehe README_PENNER_KOMBAT.md
