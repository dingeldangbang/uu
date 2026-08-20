# Inter-Fonts — Optionen (Alternative zu Downloadable Fonts)

Die App unterstützt **drei Wege**, die Inter-Schrift einzubinden.
Standard ist Weg ① (Downloadable Fonts) — ohne Eingriff.

---

## ① Downloadable Fonts (Standard — kein .ttf im Repo)

`Theme.kt` lädt die echten Inter-Glyphen zur Laufzeit über den
**Google Fonts Provider** (Google Play Services — auf dem Honeywell
CT45P standardmäßig vorhanden).

- ✅ kein Binary im Repo nötig
- ✅ automatisches Fallback auf SansSerif bei Offline/kein GMS
- ❌ benötigt GMS + Netz beim ersten Zeichnen

## ② Lokale .ttf im Repo (Alternative — offline-fest, deterministisch)

Lade die drei Dateien von https://fonts.google.com/specimen/Inter
(OFL-Lizenz, frei nutzbar) und lege sie hier ab:

```
app/src/main/res/font/
├── inter_regular.ttf     (400)
├── inter_medium.ttf      (500)
└── inter_bold.ttf        (700)
```

Danach in `Theme.kt` die Zeile

```kotlin
private fun interFontFamily(context: Context): FontFamily {
```

so anpassen, dass zuerst die Ressourcen-Fonts genutzt werden:

```kotlin
private fun interFontFamily(context: Context): FontFamily =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium,  FontWeight.Medium),
        Font(R.font.inter_bold,    FontWeight.Bold)
    )
```

(Import: `androidx.compose.ui.res.Font`)

- ✅ läuft komplett offline / ohne GMS
- ✅ deterministisch (gleiches Look auf jedem Gerät)
- ❌ ~200–300 KB Binaries im Repo

## ③ System-Fallback (Fallback-Fallback)

Passiert automatisch, wenn ① fehlschlägt → `FontFamily.SansSerif`.

---

**Empfehlung Pilot:** ① (Standard) reicht aus — das CT45P hat GMS.
Falls der Scanner ohne GMS-Provisionierung läuft, Weg ② wählen.
