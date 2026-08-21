# 🔧 CI-Reparatur (21.08.2026)

Die Actions-Badges im README waren grün-gelogen: **die CI hat faktisch nie gebaut.**
Unten stehen alle gefundenen Defekte, der Fix und wie du ihn anwendest.

## Anwenden (2 Befehle)

```bash
bash scripts/fix-workflows.sh          # patcht .github/workflows/*
git add .github/workflows && git commit -m "ci: Workflows reparieren" && git push
```

Prüfen ohne Änderung: `bash scripts/fix-workflows.sh --check` (Exit 1 = Defekt offen).
Wer lieber einen Patch anwendet: `git apply docs/ci-repair.patch` — identischer Inhalt.

> **Warum kein fertiger Commit?** Workflow-Dateien darf nur ein Token mit
> `workflows`-Berechtigung ändern. Der Agent-Token dieses Branches hat sie nicht —
> Push wird mit *„refusing to allow a GitHub App to update `.github/workflows/…`"*
> abgelehnt, die Contents-API antwortet mit `403 Resource not accessible by integration`.
> Deshalb Script + Patch statt Commit.

## Die Defekte

### 1 · `ci.yml` — ungültiger Permissions-Scope (der eigentliche Killer)

```yaml
permissions:
  artifacts: write     # existiert nicht
```

GitHub kennt die Scopes `actions, attestations, checks, contents, deployments,
discussions, id-token, issues, packages, pages, pull-requests, repository-projects,
security-events, statuses` — **kein `artifacts`**. Ein unbekannter Schlüssel macht die
gesamte Workflow-Datei ungültig: jeder Lauf endet nach 0 s als `startup_failure`,
ohne einen einzigen Job. Genau das passiert seit Monaten auf `main`.
`actions/upload-artifact` braucht ohnehin keinen eigenen Scope.

### 2 · `ci.yml` — Keystore-Decode ohne Secret

`echo "$KEYSTORE_BASE64" | base64 -d > app/secureguard-keystore.jks` schreibt auch dann
eine (leere/kaputte) Keystore-Datei, wenn das Secret fehlt. Jetzt mit `if`-Guard und
`::notice::`-Hinweis, dass unsigniert gebaut wird.

### 3 · `build-release.yml` — der Glob, der nie matcht

```bash
APK=$(ls 'app/build/outputs/apk/release/*.apk' 2>/dev/null | head -1)
```

In einfachen Anführungszeichen expandiert die Shell das `*` nicht — `ls` sucht wörtlich
nach einer Datei namens `*.apk`, findet nichts, `apk_path` bleibt leer und der
Artefakt-Upload läuft ins Leere. Fix: ohne Quotes, harter Abbruch wenn nichts gefunden
wird, und Umbenennen auf den im README dokumentierten Namen
`secureguard-pro-<tag>.apk`.

### 4 · `build-release.yml` — `{{ github.repository }}` ohne `$`

Landet als Literal im Release-Body. Fix: `${{ github.repository }}`.

### 5 · Asset-Namen != README

README verspricht `secureguard-pro-v1.0.0.apk`, die Workflows luden
`wischiwaschi-pro*.apk` hoch. Vereinheitlicht auf `secureguard-pro…` in `ci.yml`,
`build-release.yml`, `build-docker.yml`.

### 6 · `build-release.yml` — `pages: write`

Der Workflow deployt keine Pages. Scope entfernt (Least Privilege).

### 7 · `codeql.yml` — Sprache `kotlin` existiert nicht

CodeQL v3 kennt `java-kotlin` (gemeinsame Extraktor-Familie). Mit `languages: kotlin`
bricht der Init-Step ab — deshalb war auch CodeQL auf `main` dauerhaft rot.

### 8 · `badge.yml` — falsches Repo

Die ausgegebenen Badge-URLs zeigten auf `dang88bang-pixel/Dinge88` statt auf
`dang88bang-pixel/wischiwaschi-public`.

### 9 · `build-docker.yml` — Release ohne Schreibrecht

`softprops/action-gh-release@v2` hängt Assets an ein Release, der Default-Token ist bei
PR-/Push-Läufen aber read-only. `permissions: contents: write` ergänzt.

## Danach

Sobald die Workflows gültig sind, laufen `lint`, `test` und `assemble` durch und
verifizieren die Permission-Änderungen aus diesem Branch (PR #14). Erwartbar ist
dabei noch ein **inhaltlicher** Erstlauf-Befund: `:app:testDebugUnitTest` findet
aktuell keine Tests (`app/src/test/` existiert nicht) — das ist kein Fehler, aber
die Badge „Tests" bedeutet damit bislang nichts.
