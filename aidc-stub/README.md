# aidc-stub — Honeywell DataCollection SDK (Build-Zeit-Stub)

Das echte Honeywell-AIDC-SDK (CT45P-Scanner) wird nur über das
**Honeywell Tech-Portal** als AAR verteilt und liegt **nicht** in
öffentlichen Maven-Repos. Dieses Modul stellt die minimal benötigte
API-Oberfläche (`com.honeywell.aidc.*`) bereit, damit das Projekt
ohne SDK baut:

| Symbol | Stub-Verhalten |
| --- | --- |
| `BarcodeReader.from(ctx)` | liefert immer `null` |
| `BarcodeListener` / `TriggerListener` | Interfaces vorhanden, No-Ops |
| `claim()` / `release()` / `softScan*()` | idempotente No-Ops |

Konsequenz zur Laufzeit: `HoneywellScanner.isAvailable()` meldet
ehrlich `false` und die App läuft auf Nicht-Honeywell-Geräten stabil.

## Echtes SDK einbinden

1. AAR aus dem Honeywell Tech-Portal herunterladen
   („DataCollection SDK", passend zur CT45P-Firmware).
2. AAR nach `app/libs/aidc.aar` legen.
3. In `app/build.gradle` ersetzen:

   ```gradle
   implementation project(':aidc-stub')
   ```
   durch
   ```gradle
   implementation fileTree(dir: 'libs', include: ['*.aar'])
   ```
4. Optionaler CI-Check: `honeywell-experimental.yml` (workflow_dispatch)
   baut mit dem AAR aus dem Secret `HONEYWELL_AAR_BASE64`.
