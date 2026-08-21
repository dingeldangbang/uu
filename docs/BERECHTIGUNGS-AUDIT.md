# 🔐 Berechtigungs-Audit (Stand 2026-08-21) — **alle Befunde behoben**

Geprüft: `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `BLEService`, `WiFiService`,
`SatelliteService`, `UrbanService`, `NotificationService`, `QrScanScreen`, `OpticalScanScreen`.
minSdk 26 · targetSdk/compileSdk 34 · Zielgerät CT45P (Android 11 / API 30).

## Übersicht

| Permission | Manifest | Runtime-Request | Im Code genutzt | Bewertung |
| --- | --- | --- | --- | --- |
| ACCESS_FINE_LOCATION | ✅ | ✅ MainActivity | BLE/WiFi/GNSS/Urban | ok |
| ACCESS_COARSE_LOCATION | ✅ | ✅ | Satellite/Urban | ok |
| ACCESS_BACKGROUND_LOCATION | ✅ | ❌ nie | ❌ | **B1 – entfernen** |
| BLUETOOTH / BLUETOOTH_ADMIN (max 30) | ✅ | – (normal) | BLEService | ok |
| BLUETOOTH_SCAN / _CONNECT | ✅ | ✅ ab API 31 | BLEService | **B2 – `neverForLocation`-Flag fehlt** |
| INTERNET / ACCESS_NETWORK_STATE | ✅ | – | ApiNodeManager | ok |
| CAMERA | ✅ | ✅ (2×: Start + Screen) | Optical/QR | **B3 – Vorab-Request beim App-Start** |
| ACCESS_WIFI_STATE / CHANGE_WIFI_STATE | ✅ | ⚠️ im Runtime-Batch | WiFi/Urban | **B4 – normal, Request wirkungslos** |
| READ_PHONE_STATE (≤ API 32) | ✅ | ✅ | UrbanService-Gate | **B5 – falsches Gate** |
| READ_BASIC_PHONE_STATE (33+) | ✅ | ⚠️ im Runtime-Batch | UrbanService-Gate | **B4/B5 – normal, kein Dialog** |
| NEARBY_WIFI_DEVICES (33+) | ✅ | ✅ | WiFiService | **B2 – `neverForLocation` fehlt** |
| POST_NOTIFICATIONS | ✅ | ✅ (2×) | NotificationService | ok |
| FOREGROUND_SERVICE(+_LOCATION,+_DATA_SYNC) | ✅ | – | ❌ kein `Service`/`setForeground` | **B6 – ungenutzt** |
| RECEIVE_BOOT_COMPLETED | ✅ | – | ❌ kein Receiver | **B6 – ungenutzt** |
| WAKE_LOCK | ✅ | – | nur indirekt (WorkManager) | ok |
| com.honeywell.aidc.RETRIEVE_DATA | ✅ | – | AIDC-SDK | ok (OEM-Image) |

## Befunde

### B1 · `ACCESS_BACKGROUND_LOCATION` deklariert, aber nie angefordert
`MainActivity` dokumentiert bewusst, dass sie nicht angefragt wird — die Deklaration steht
trotzdem im Manifest. Folge: Play-Console verlangt eine Background-Location-Deklaration
(Review-Blocker), ohne dass die App den Nutzen hat. **Empfehlung:** Zeile entfernen.

### B2 · BLE-/WiFi-Scan-Permissions ohne `usesPermissionFlags="neverForLocation"`
Ohne das Flag bleiben `BLUETOOTH_SCAN` und `NEARBY_WIFI_DEVICES` an `ACCESS_FINE_LOCATION`
gekoppelt. Da die App Standort ohnehin nutzt, ist das funktional unkritisch, für den
Datenschutz-Teil der Betriebsvereinbarung aber relevant zu dokumentieren.

### B3 · `CAMERA` wird beim App-Start pauschal abgefragt
`RequestRequiredPermissions()` fragt CAMERA direkt beim ersten Composition an, obwohl
`OpticalScanScreen`/`QrScanScreen` sie kontextbezogen selbst anfordern. Der Vorab-Dialog ohne
sichtbaren Nutzen erhöht die Ablehnungsquote und widerspricht der Android-Guideline
„Permissions in Context". **Empfehlung:** CAMERA aus dem Start-Batch entfernen.

### B4 · Normale Permissions im Runtime-Batch
`ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` und `READ_BASIC_PHONE_STATE` haben Protection-Level
*normal* — `RequestMultiplePermissions` liefert sie sofort als „granted" zurück, ohne Dialog.
Kein Fehler, aber toter Code/irreführender Kommentar.

### B5 · Falsches Permission-Gate in `UrbanService.scanCellTowers()`
`TelephonyManager.getAllCellInfo()` verlangt **`ACCESS_FINE_LOCATION`**, nicht Phone-State.
Aktuell wird über `hasPhonePermission()` gegatet:
* API ≤ 32: Nutzer lehnt `READ_PHONE_STATE` ab → Zell-Scan wird abgeschaltet, obwohl er
  mit Location funktionieren würde (falscher Feature-Verlust, betrifft das CT45P-Zielgerät).
* API ≥ 33: `READ_BASIC_PHONE_STATE` ist immer granted → das Gate prüft faktisch nichts;
  ohne Location gibt es nur eine gefangene `SecurityException`/leere Liste.

**Empfehlung:** in `scanCellTowers()` auf Location prüfen (`hasWifiPermission()`-Logik bzw.
eigene `hasLocationPermission()`), Phone-State-Prüfung nur zusätzlich.

### B6 · Ungenutzte Permissions
Weder ein `Service` noch `setForeground`/`ForegroundInfo` (Worker) existiert — die vier
`FOREGROUND_SERVICE*`-Einträge und `RECEIVE_BOOT_COMPLETED` (kein `BOOT_COMPLETED`-Receiver)
sind ohne Wirkung. Entweder entfernen oder den fehlenden Foreground-Service/Receiver nachziehen
(sonst läuft „Tracking im Hintergrund" faktisch nicht).

### Kleinere Punkte
* `SatelliteService.hasPermission()` prüft ab API 31 **nur** FINE. Erteilt der Nutzer nur
  „ungefährer Standort", liefert der Service gar nichts, obwohl COARSE genügen würde.
  Besser: `FINE || COARSE`.
* `BLEService` liest `result.device?.name` (Zeilen 143/160) — erfordert ab API 31
  `BLUETOOTH_CONNECT`. Praktisch mitgewährt (Gruppe „Geräte in der Nähe"), sauber wäre eine
  eigene Prüfung bzw. `try/catch` um den Namenszugriff.
* Es gibt keine Rationale-/Denied-Behandlung: Ergebnis-Callback in `MainActivity` ist leer,
  bei dauerhafter Ablehnung sterben Suchkanäle stumm. Ein Status-Panel („Kanal X inaktiv –
  Berechtigung fehlt") in den Settings wäre die günstigste Verbesserung.

## Umsetzung (alle Punkte erledigt)

| Befund | Fix |
| --- | --- |
| B1 | `ACCESS_BACKGROUND_LOCATION` aus dem Manifest entfernt (+ Begründung als Kommentar) |
| B2 | Bewusst **kein** `neverForLocation`: die App leitet aus BLE/WLAN Standort ab — das Flag wäre eine Falschaussage. Im Manifest dokumentiert. |
| B3 | `CAMERA` aus dem Start-Batch entfernt; bleibt Permission-in-Context in den Scan-Screens |
| B4 | Normale Permissions (`ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `READ_BASIC_PHONE_STATE`) nicht mehr im Runtime-Batch |
| B5 | `UrbanService.scanCellTowers()` gated jetzt auf FINE/COARSE-Location; `hasWifiPermission()` akzeptiert Location **oder** `NEARBY_WIFI_DEVICES` (33+) |
| B6 | `FOREGROUND_SERVICE*` und `RECEIVE_BOOT_COMPLETED` entfernt — es gibt keinen Service/Receiver, WorkManager re-scheduled nach Reboot selbst |
| Klein | `SatelliteService`: FINE **oder** COARSE; Priority je nach Genauigkeit. `BLEService`: `safeDeviceName()` prüft `BLUETOOTH_CONNECT` + `runCatching`. Neues `util/PermissionStatus.kt` als Single Source of Truth. Settings-Panel „Berechtigungen" zeigt pro Kanal den Status, kann fehlende Rechte nachfordern oder die App-Einstellungen öffnen. |

> ⚠️ Offene Produktentscheidung: Ohne Foreground-Service und Boot-Receiver läuft Tracking
> **ausschließlich im Vordergrund** bzw. über WorkManager (Intervall ≥ 15 min). Soll echtes
> Hintergrund-Tracking kommen, müssen Service + FGS-Permissions + FGS-Typen wieder rein —
> und die Betriebsvereinbarung entsprechend erweitert werden.

## Ursprüngliche Priorisierung

1. **B5** (Funktionsfehler auf dem Zielgerät) und **B6** (Foreground-Service fehlt komplett)
2. **B1**, **B3** (Store-/Datenschutz-Risiko)
3. **B2**, **B4**, Kleinere Punkte (Hygiene)

---

## Nachtrag: Build-/CI-Verifikation (21.08.2026)

**Lokaler Build nicht möglich.** Die Arbeitsumgebung erreicht weder `dl.google.com`
(Android SDK, Google Maven) noch `repo.maven.apache.org` oder `services.gradle.org`.
Ohne diese Quellen gibt es kein `android.jar`, keine AGP/Compose-Artefakte und keine
Gradle-Distribution — `./gradlew compileDebugKotlin` kann dort prinzipiell nicht laufen.
`make doctor` (neu) meldet genau das in zwei Sekunden statt nach zehn Minuten Timeout.

**CI konnte es ebenfalls nicht verifizieren — aus einem eigenständigen Bug:**
`.github/workflows/ci.yml` deklariert

```yaml
permissions:
  artifacts: write     # ← existiert nicht
```

`artifacts` ist **kein gültiger GitHub-Permissions-Scope**. Dadurch ist die komplette
Workflow-Datei ungültig und jeder Lauf endet sofort als `startup_failure` —
auch auf `main`. Die grünen Badges im README täuschen: die CI hat faktisch nie gebaut.

**Fix (ein Zeilen-Delete), muss von Hand erfolgen** — der Agent-Token dieses Branches
hat keine `workflows`-Berechtigung und darf Workflow-Dateien nicht pushen:

```diff
 permissions:
   contents: read
   packages: read
   actions: read
-  artifacts: write
```

`actions/upload-artifact` braucht keinen eigenen Scope. Danach laufen Lint, Unit-Tests
und der Debug-/Release-Build durch und verifizieren die Permission-Änderungen aus diesem
Branch.
