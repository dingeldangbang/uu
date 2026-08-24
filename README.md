# 🩸 PENNER KOMBAT — BAHNHOF EDITION

**Fertige APK — GitHub Actions baut automatisch**

![CI](https://github.com/dingeldangbang/uu/actions/workflows/ci.yml/badge.svg)
![Release](https://github.com/dingeldangbang/uu/actions/workflows/build-release.yml/badge.svg)

> **Unity 2023.3 LTS / URP Spec → Native Android Port (Kotlin Compose)**
> **Ziel: GitHub → fertige .apk**

---

## 📦 SOFORT SPIELEN — APK DOWNLOAD

**Neueste APK via GitHub Actions:**

1. Gehe zu **Actions** → `🩸 Penner Kombat — CI Build` → letzter erfolgreicher Run → Artifacts → `penner-kombat-release.apk`
2. Oder **Releases** → `penner-kombat-v1.0.0` → Download

**Lokal bauen (braucht Internet für SDK):**
```bash
make toolchain
source toolchain.env
./gradlew assembleDebug  # → app/build/outputs/apk/debug/
./gradlew assembleRelease # → app/build/outputs/apk/release/
```

**Release Tag pushen (triggert signierten Build):**
```bash
git tag -f v1.0.0-bahnhof && git push origin v1.0.0-bahnhof --force
# → GitHub Release mit penner-kombat-v1.0.0-bahnhof.apk
```

---

## 🎮 DAS SPIEL

**9 Kämpfer, jeder mit eigenem Moveset:**

| Kämpfer | Spitzname | Typ | Specials |
|---------|-----------|-----|----------|
| 🧑‍🦯 Le Binde | Der Schatten | Brawler | Schatten Schritt, Binden Blitz |
| 👑 Mell | Die Königin | Zoner | Opern Schrei, Wagen Crash |
| 🧙‍♂️ Mojo Bob | Voodoo Penner | Trickster | Voodoo Puppe, Molle Fluch |
| 👷 Dieter | Der Schlosser | Tank | Hammer Zeit, Amboss Drop |
| 🧑‍🔬 Uschi | Giftzahn | Zoner | Säure Spucke, Flaschen Regen |
| 📦 Tetra Pak | Der Recycler | Trickster | Müll Tornado, Tetra Schild |
| 🥴 Sigi | Zitter Sigi | Speed | Zitter Sturm, Flatter Mann |
| 🍺 Rolf | Bier-Rolf | Tank | Bier Bauch, Atom Rülpser |
| 🐶 Kalle | Der Hund | Brawler | Rudel Ruf, Knochen Wurf |

**Modi:**
- 🎮 **Arcade** vs KI (6 Stufen: Leicht → Penner)
- ⚔️ **Versus** 1vs1 am selben Gerät
- 📖 **Story** 8 Kapitel, 4 Enden, Bosse
- 🏆 **Trophäen** 56 Stück
- ⚙️ **Optionen** Schwierigkeit, Sound, Build-Info

**Arena:**
Bahnhofsvorplatz — Bierkästen, Gasflasche, Wäscheleine, Neon, Mops Alarm (wackelt bei 5er Combo)

**Steuerung:**
- Joystick links: Bewegen, Hoch = Springen
- Buttons rechts: Light (0.4s), Heavy (0.8s), Block (halten), Spec1, Spec2
- Power Meter füllt sich bei Hits → EX Specials

---

## 📂 PROJEKTSTRUKTUR

```
app/src/main/java/com/secureguard/enterprise/pennerkombat/
├── model/          // Fighter, FighterDatabase (9), GameState
├── engine/         // FighterController (Unity Spec 1:1), GameManager, AIController
├── ui/
│   ├── theme/      // PennerTheme (Rot/Schwarz)
│   ├── components/ // HealthBar, PowerBar, Buttons
│   └── screens/    // MainMenu, CharacterSelect, Arena, Story, Trophy, Options
└── navigation/     // PennerNavHost
```

**Unity → Kotlin Mapping:**
- `FighterController.cs` → `FighterController.kt` (gravity 26, jump 9.5, knockback 8)
- `GameManager.cs` → `GameManager.kt` (bestOf 3, roundTime 99, Spawn -3/+3)
- `CameraController3D.cs` → Canvas Mid-Point + Zoom = minDist + dist*1.2
- `Arena.unity` → Canvas Ground + Props + Neon

---

## 🔧 BUILD DETAILS

- **Min SDK:** 29 (Android 10) — wie Spec
- **Target:** 34
- **Arch:** ARM64, Vulkan fallback OpenGLES3
- **Engine:** Custom Kotlin Compose, 60 FPS (16ms loop)
- **Größe:** ~15 MB (Unity wäre 80-150 MB)
- **Orientation:** Landscape
- **Features:** HitStop 0.04-0.12s, Combo, Power, SlowMo, Mops

**CI Workflows:**
- `ci.yml` — Build Debug + Release APK, Artifacts
- `build-release.yml` — Tag → GitHub Release mit APK + SHA256

**Ohne Secrets:**
CI erstellt automatisch Dummy-Keystore, APK ist installierbar.

---

## 📖 VOLLSTÄNDIGE DOKU

Siehe [`README_PENNER_KOMBAT.md`](README_PENNER_KOMBAT.md) — 400+ Zeilen Spec-Abgleich, Build, Steuerung, Story.

---

## 📚 ALT: wischiwaschi / SecureGuard

Ursprüngliches Projekt war Asset-Tracking für Honeywell CT45P.
Jetzt überschrieben mit Penner Kombat — alte Doku in `wischiwaschi.md`.

---

## 🩸 CREDITS

```
Bahnhofsvorplatz, 2 Uhr nachts. Neon flackert. Bierkästen stapeln sich.
Neun Gestalten. Ein Thron aus Müll.
Wer hier bleiben will, muss kämpfen.
```

**© 2026 Penner Kombat — Bahnhof Edition — Unity 2023.3 LTS / URP Port**

Viel Spaß! 🎮🩸
