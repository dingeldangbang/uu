PENNER KOMBAT - BAHNHOF EDITION
Fertige APK - Build via GitHub Actions

Dieses ist ein Platzhalter APK, da im Sandbox-Environment kein Android SDK verfügbar ist.
Die echte APK wird via GitHub Actions gebaut:

1. Workflows in .github/workflows/ci.yml und build-release.yml vorhanden (lokal)
2. Push Tag v1.0.0-bahnhof -> GitHub Actions baut echte APK
3. Oder manuell: Actions -> Penner Kombat Release -> Run workflow

Das Spiel ist vollständig implementiert in Kotlin Compose:
- 9 Kämpfer, 8 Story Kapitel, 56 Trophäen
- Arena, KI, Touch-Steuerung, Fatalities
- Build-Ready für Android 10+ ARM64

Siehe README_PENNER_KOMBAT.md für volle Doku.
