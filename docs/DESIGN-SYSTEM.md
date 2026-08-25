# 🎨 Design-System — Industrial Precision 2.0

Referenz: Stitch-Projekt „AccessOps" (Zutritts-/Anlagen-Operationssystem).

Die App nutzt die Palette **„Industrial Precision 2.0"** als Licht-/Dunkel-Schema
und **Inter** als Hausschrift.

## Farbpalette

| Rolle | Light | Dark |
|-------|-------|------|
| Primary | `#005EB8` | `#A9C7FF` |
| Secondary | `#1A1C1E` | `#C6C6C9` |
| Tertiary | `#EE3124` | `#FFB4A9` |
| Neutral / Background | `#F8F9FA` | `#1A1C1E` |
| Neutral (Text sekundär) | `#44484C` | `#76777B` |

Umsetzung in der App:

- **Compose:** `presentation/theme/Theme.kt` → `SecureGuardTheme` (Material3-Schemata)
- **XML/System-UI:** `res/values/colors.xml` → StatusBar, NavigationBar, Splash, Launcher-Icon

## Typografie

- **Inter** (Normal / Medium / SemiBold / Bold) via Google Fonts Provider
  (Downloadable Fonts), Fallback: `SansSerif`.
- Typo-Skala: Material3-Typography, vollständig auf Inter gesetzt.

## Sonstige Token

- **Splash-/Launcher-Icon:** Schild-Motiv — Primary-Blau auf weißem Schild,
  neutraler Hintergrund `#1A1C1E` (Splash) bzw. `#005EB8` (Adaptive-Icon-Hintergrund).
- **Semantikfarben:** `ok` = `#2E7D32` (Grün), `warn`/`error` = Tertiary `#EE3124` (Rot).

## Funktions-Referenz (Stitch-Screens → App)

Die Stitch-Referenz definiert u. a. Live-Lagebild v3.0, Diagnose-Center, RBAC-Matrix,
Geräte-Datenbank und Akku-Token-Triangulation. Die App bildet die Feldeinsatz-Perspektive
auf dem CT45P ab: Dashboard (Live-Lagebild), Assets, Karte (Triangulation/Ortung),
Aktionen (Fernsteuerung), Agent, Settings — thematisch gekoppelt an die
OSDP-/Zutritts- und Tracking-Welt der Referenz.
