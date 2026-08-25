# 🚀 NEXT STEPS — SecureGuard Enterprise Release v1.0.0

**Stand 2026-08-25:** Session-Branch `arena/01a036a3-uu` (Nachfolger von
`arena/01a03677-uu`). Der Übergabe-Commit `ec1c57e` der Vor-Session war wegen der
Remote-Sperre **nie gepusht** worden und wurde in dieser Session rekonstruiert.

**Rechte-Status (verifiziert):** Der Arena-GitHub-App fehlen weiterhin die
`workflows`- und `secrets`-Berechtigungen (Push von `.github/workflows/*` wird
abgelehnt: *„refusing to allow a GitHub App to create or update workflow … without
`workflows` permission“*; `gh secret set` → HTTP 403). **→ Admin-Fallback ist
aktiv: Schritt 2 + 3 macht der Mensch, den Rest die CI.**

> ⚠️ **Keystore-Hinweis:** `signing/` ist gitignored und lag nur im Sandbox der
> geschlossenen Session → der alte Keystore ist verloren. Der neue Keystore liegt
> unter `signing/secureguard-keystore.p12` (PKCS12, Alias `secureguard`, frische
> Signatur-Identität, gleiche Struktur). Diese Dateien existieren **nur im
> Workspace der Session** — bitte herunterladen und sicher ablegen (GitHub wird
> sie nie enthalten).

---

## ✅ Erledigt

- App auf `main`: SecureGuard Enterprise v1.0.0 (PRs #3–#5)
- Vorlagen: `docs/workflows/{ci,build-release}.yml` + Admin-Skripte in `scripts/`
- Rekonstruiert (diese Session):
  - `signing/` → `secureguard-keystore.p12`, `keystore.b64`, `secrets.txt`
  - `NEXT-STEPS.md` (diese Datei, gepusht über den Agent-Branch)

## 📋 Offene Schritte (exakte Checkliste)

1. [ ] **Schritt 2 — Workflows aktivieren** (Admin, Einzeiler):
   ```bash
   bash scripts/install-workflows.sh && git add .github/workflows && git commit -m "ci: Workflows aktivieren" && git push
   ```
2. [ ] **Schritt 3 — Secrets setzen** (Admin, Einzeiler):
   ```bash
   bash scripts/set-release-secrets.sh signing/secureguard-keystore.p12
   ```
   Passwörter aus `signing/secrets.txt` entnehmen.
3. [ ] **Tag pushen** (löst den Release-Workflow aus):
   ```bash
   git tag v1.0.0 && git push origin v1.0.0
   ```
4. [ ] **Prüfen:** GitHub-Release `v1.0.0` mit signierter **`app-release.apk`**
   + `SHA256SUMS.txt` (Prüfsumme).

Die CI macht den Rest automatisch: Tag → Workflow **Release** baut die signierte
APK und veröffentlicht sie als GitHub-Release inkl. Prüfsumme.
