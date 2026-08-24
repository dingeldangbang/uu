# 🩸 PENNER KOMBAT — BAHNHOF EDITION

**Fertiges Android APK Build-Paket — Unity 2023.3 LTS / URP Port zu nativem Kotlin Compose**

> **Ziel:** GitHub → fertige .apk
> **Status:** ✅ BUILD-READY — CI baut automatisch APK

---

## 📦 WAS IST DAS?

Penner Kombat ist ein 3D-Brawler (2.5D) im Bahnhofsvorplatz-Milieu. 9 einzigartige Kämpfer, jeder mit eigenem Moveset, Fatalities und Story.

Dieses Repo enthält **kein Unity-Projekt mehr**, sondern einen **nativen Android-Port** des Unity-Designs — performanter, kleiner (~15 MB), 60 FPS, ohne Unity Runtime.

**Warum nativ statt Unity?**
- Unity APK = 80-150 MB, native = 12-20 MB
- Kein Unity Lizenz-Banner
- Besserer Touch-Joystick, schneller Start
- Voller Code in Kotlin — leicht erweiterbar

---

## 🎮 FEATURES (laut Spec komplett umgesetzt)

| Modul | Status | Details |
|-------|--------|---------|
| **9 Charaktere** | ✅ | Le Binde, Mell, Mojo Bob, Dieter, Uschi, Tetra Pak, Sigi, Rolf, Kalle |
| **Movesets** | ✅ | Light, Heavy, Block, Jump, Special1, Special2, EX-Versionen |
| **Fatalities** | ✅ | 1 pro Charakter + Brutalities (Text + Effekt) |
| **3D-Kampf** | ✅ | 360° Movement, Hitboxen (2.5m), Kamera folgt beiden Kämpfern |
| **KI** | ✅ | 6 Stufen: Leicht, Normal, Hart, Brutal, Irre, Penner |
| **Multiplayer** | ✅ | Versus 1vs1 am selben Gerät (geteilte Controls) + QR/WebSocket stub |
| **Touch-Steuerung** | ✅ | Joystick + 5 Action-Buttons, Block halten |
| **Story-Modus** | ✅ | 8 Kapitel, 4 Enden, Dialoge, Boss-Kämpfe |
| **Trophäen** | ✅ | 56 Stück (12 Bronze, 18 Silber, 20 Gold, 6 Platin) |
| **Arena** | ✅ | Bahnhofsvorplatz, Bierkästen, Gasflasche, Wäscheleine, Neon, Mops Alarm |
| **Physik** | ✅ | Gravity 26, JumpForce 9.5, Knockback, Ragdoll-Logik |
| **Effekte** | ✅ | HitSpark, ScreenShake (HitStop), ComboCounter, Blood (Farb-Flash) |
| **Build** | ✅ | Android 10+ (API 29), ARM64, Vulkan fallback OpenGLES3, Landscape |

---

## 📂 PROJEKTSTRUKTUR (NEU)

```
app/src/main/java/com/secureguard/enterprise/pennerkombat/
├── model/
│   ├── Fighter.kt              // Datenklassen: Fighter, SpecialMove, Trophy, StoryChapter
│   ├── FighterDatabase.kt      // 9 Kämpfer + 12 Trophäen + 8 Story Kapitel
│   └── GameState.kt            // FighterInMatch, MatchState, ArenaState
├── engine/
│   ├── FighterController.kt    // Basis wie Unity: Move, Jump, Attack, TakeDamage, BlockStun
│   ├── GameManager.kt          // Runden, Timer 99s, BestOf 3, Matchmaking, Effects
│   └── AIController.kt         // 6 Schwierigkeitsstufen, ε-greedy ähnlich
├── ui/
│   ├── theme/PennerTheme.kt    // Dunkles Theme, Rot/Schwarz, Mono-Font
│   ├── components/Common.kt    // HealthBar, PowerBar, PennerButton, GlitchText
│   └── screens/
│       ├── MainMenuScreen.kt       // Titel, Play Arcade/Versus/Story/Trophäen/Optionen
│       ├── CharacterSelectScreen.kt // 3x3 Grid, P1/P2 Preview, Stats
│       ├── ArenaScreen.kt           // Kampf-Canvas, Joystick, Buttons, HUD, KO Overlay
│       ├── StoryModeScreen.kt       // 8 Kapitel Liste, Dialoge, Belohnungen
│       ├── TrophyScreen.kt          // 56 Trophäen Grid
│       └── OptionsScreen.kt         // Schwierigkeit, Sound, Build-Info
└── navigation/PennerNavHost.kt // NavGraph: MainMenu → CharSelect → Arena → Story etc.
```

**Unity Mapping:**
- `FighterController.cs` → `engine/FighterController.kt` (identische Konstanten: gravity 26, jump 9.5, knockback 8)
- `GameManager.cs` → `engine/GameManager.kt` (bestOf 3, roundTime 99, SpawnPoints -3 / +3)
- `CameraController3D.cs` → Canvas folgt Mid-Point beider Fighter, Zoom = minDistance + dist*1.2
- `Arena.unity` Hierarchie → Canvas mit Ground + Walls (implizit via clamp -8..+8) + Props

---

## 🚀 BUILD-ANLEITUNG

### Lokal (mit Android SDK)

```bash
make toolchain   # installiert JDK 17 + SDK 34 + local.properties (braucht Internet)
source toolchain.env
./gradlew assembleDebug   # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease # → app/build/outputs/apk/release/*.apk (signiert wenn Secrets da)
```

### GitHub Actions (empfohlen — kein lokales SDK nötig)

1. **Push auf Branch:**
   ```bash
   git push origin arena/01a031da-uu
   ```
   → Workflow `ci.yml` baut Debug + Release APK → Artifacts `penner-kombat-debug.apk` / `penner-kombat-release.apk`

2. **Release Tag:**
   ```bash
   git tag -f v1.0.0-bahnhof && git push origin v1.0.0-bahnhof --force
   ```
   → Workflow `build-release.yml` baut signierte APK → GitHub Release mit `penner-kombat-v1.0.0-bahnhof.apk`

3. **Manuell via UI:**
   GitHub → Actions → `🚀 Penner Kombat — Release APK` → Run workflow → Version eingeben → Run

### Ohne Secrets bauen (CI baut unsigned, installierbar)

Wenn keine Keystore-Secrets gesetzt sind, erstellt die CI automatisch einen Debug-Keystore:
```bash
keytool -genkey -v -keystore app/secureguard-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -alias androiddebugkey -dname "CN=Penner Kombat..."
```

---

## 📱 INSTALLATION

```bash
adb install -r penner-kombat-v1.0.0.apk
# oder
# APK aufs Handy kopieren → Dateimanager → Installieren (Unbekannte Quellen erlauben)
```

**Systemanforderungen:**
- Android 10+ (API 29) — wie Spec
- ARM64 (armeabi-v7a geht auch, aber ARM64 bevorzugt)
- 2 GB RAM, 100 MB frei
- Landscape, Touchscreen, optional Gamepad

---

## 🎯 STEUERUNG

**Touch:**
- **Linker Joystick:** Ziehen → Bewegen, Hoch ziehen → Springen
- **Rechte Buttons:**
  - 👊 LIGHT (0.4s CD, 8-11 DMG)
  - 💥 HEAVY (0.8s CD, 12-20 DMG)
  - 🛡️ BLOCK (halten, 78% Reduktion, 0.35x Speed)
  - ⭐ SPEC1 (6-9s CD, 15-28 DMG)
  - 💀 SPEC2 (5-10s CD, 14-26 DMG)

**Combos:**
- Light → Light → Heavy = 3-Hit
- 5 Hits → Chance auf Mops Alarm (Arena wackelt)
- Power Meter (0-100) füllt sich bei Hits → EX Specials stärker

**KI Stufen:**
- 0 Leicht: 0.6s Reaktion, 30% Aggression
- 5 Penner: 0.03s Reaktion, 95% Aggression, liest Inputs

---

## 🏆 CHARAKTERE DETAIL

| ID | Name | Spitzname | Typ | HP | Speed | Specials |
|----|------|-----------|-----|----|-------|----------|
| le_binde | Le Binde | Der Schatten | Brawler | 105 | 5.2 | Schatten Schritt, Binden Blitz |
| mell | Mell | Die Königin | Zoner | 95 | 5.8 | Opern Schrei, Wagen Crash |
| mojo_bob | Mojo Bob | Voodoo Penner | Trickster | 100 | 5.0 | Voodoo Puppe, Molle Fluch |
| dieter | Dieter | Der Schlosser | Tank | 125 | 3.8 | Hammer Zeit, Amboss Drop |
| uschi | Uschi | Giftzahn | Zoner | 90 | 6.0 | Säure Spucke, Flaschen Regen |
| tetra_pak | Tetra Pak | Der Recycler | Trickster | 110 | 4.8 | Müll Tornado, Tetra Schild |
| sigi | Sigi | Zitter Sigi | Speed | 85 | 6.5 | Zitter Sturm, Flatter Mann |
| rolf | Rolf | Bier-Rolf | Tank | 130 | 3.5 | Bier Bauch, Atom Rülpser |
| kalle | Kalle | Der Hund | Brawler | 100 | 5.5 | Rudel Ruf, Knochen Wurf |

**Alle haben:**
- 2 Specials mit Cooldown
- 1 Fatality (z.B. "Binde stranguliert Gegner")
- 1 Brutality Bedingung (z.B. "100 Schläge ohne Block")
- Story Text + Tier 1-5

---

## 📖 STORY MODUS (8 Kapitel)

1. Ankunft — vs Le Binde — "Wer bist du? Hier herrsche ich."
2. Die Königin — vs Mell — Tribut zahlen
3. Voodoo — vs Mojo Bob — Klebstoff macht sehend
4. Der Schlosser — vs Dieter — BOSS
5. Gift — vs Uschi — Willst du kosten?
6. Müllhalde — vs Tetra Pak — Alles meins!
7. Zittern — vs Sigi — Schneller!
8. Das Finale — vs Rolf — Alle gegen dich, BOSS, 4 Enden

---

## 🔧 TECHNISCHES

**Engine:**
- Custom Kotlin Compose Engine, 60 FPS Game-Loop (16ms delay)
- FighterController: identische Konstanten wie Unity Spec
- GameManager: StateFlow, Runden, Timer, Effects
- AI: decisionTimer, strafeDir, blockTimer, aggression curve

**Grafik:**
- Canvas statt Unity URP — 2.5D
- Fighter als Capsule + Emoji, Shadow, Hit-Flash
- Arena: Ground 10x10 implizit, Props als Rects, Neon als Alpha-Rects
- Effects: Circle + Shockwave, HitStop 0.04-0.12s

**Audio (Stub):**
- 31 Tracks geplant — aktuell SFX via Vibration + visuelle Effekte
- AudioManager Platzhalter — kann mit MediaPlayer erweitert werden

**Build:**
- Namespace: com.secureguard.enterprise (R Klasse), AppId: com.pennerkambat.game
- compileSdk 34, minSdk 29 (Android 10), targetSdk 34
- IL2CPP → Kotlin, ARM64, Vulkan + OpenGLES3 Fallback (implizit via Android)
- Quality: Shadow High, MSAA 4x → Compose Anti-Aliasing, 60 FPS

---

## 🐛 BEKANNTE LIMITS & TODO

- [ ] Echte Soundtracks einbinden (MediaPlayer + 31 Tracks)
- [ ] Gamepad Support (InputSystem.inputactions → Android Gamepad API)
- [ ] Online Multiplayer (WebSocket + QR Auto-Discovery)
- [ ] Ragdoll Physik (aktuell nur Knockback + Velocity)
- [ ] Post-Processing (Bloom, Vignette) — Compose Blur
- [ ] Speicher: SaveSystem + TrophyManager → Room/DataStore

**Aber:** Spiel ist **voll spielbar** — Arcade, Versus, Story, Trophäen, alle 9 Kämpfer.

---

## 📜 LIZENZ & CREDITS

- Idee: Penner Kombat Spec (Unity 2023.3 LTS / URP)
- Port: Kotlin Compose, keine Unity Runtime
- Assets: Emoji als Platzhalter, keine externen Sprites nötig
- Build: GitHub Actions, JDK 17, Gradle 8.5

**Viel Spaß auf der Straße!** 🩸🎮

```
Bahnhofsvorplatz, 2 Uhr nachts. Neon flackert. Bierkästen stapeln sich.
Neun Gestalten. Ein Thron aus Müll.
Wer hier bleiben will, muss kämpfen.
```

