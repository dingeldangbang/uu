# 🛡️ SecureGuard — Betriebsvereinbarung (DSGVO / BDSG)

> **⚠️ STATUS: BLAUPAUSE — NICHT ANGEBUNDEN (Pilotphase)**  
> Diese Datei dient **ausschließlich** als Compliance-bezogenes Referenz­
> dokument. Im laufenden Pilotprojekt wird sie von der App **nicht
> eingelesen, nicht im UI angezeigt, nicht zur Akzeptanz abgefragt und
> nicht gegen den Code asserted**. Siehe `docs/ARCHITEKTUR.md`
> (Klasse-Doc): "*Was diese Application NICHT tut*".  
>
> **Aktiv-TOMs aus dieser Blaupause sind bereits implementiert**  
> (siehe `README.md` → Architektur → DSGVO / TOMs). Was noch fehlt, ist
> die **formelle Akzeptanz** und das **Assessor-Sign-off** — das wird
> erst produkt­gekoppelt (Roll-Out beim Verlassen der Pilotphase).

---

Diese Vereinbarung regelt die Verarbeitung personenbezogener Daten durch
die **SecureGuard**-Anwendung gemäß DSGVO und BDSG (insb. § 26 BDSG).

## § 1 — Verantwortlicher

Verantwortlich im Sinne von Art. 4 Nr. 7 DSGVO ist der Betreiber der
jeweiligen Installation. SecureGuard dient als reines Werkzeug und
verarbeitet Daten weisungsgebunden.

## § 2 — Verarbeitete Datenkategorien

| Kategorie | Zweck | Speicherort | Rechtsgrundlage |
|-----------|-------|-------------|------------------|
| GPS-/GNSS-Positionen | Asset-Tracking | Lokales SQLite (AES) | Berechtigtes Interesse nach Art. 6 Abs. 1 lit. f DSGVO |
| Erkennungsereignisse | Sicherheitsanalyse | Lokales SQLite | Berechtigtes Interesse |
| Geräte-IDs / IMEI-Hash | Hardware-Bindung | Lokal (SHA-256) | Vertragserfüllung |
| LoRa-Telemetrie | Asset-Kommunikation | Lokal | Berechtigtes Interesse |
| Crowdsourcing-Beiträge | Anonymisierte Signale | Weitergabe nur anonymisiert | Einwilligung |

## § 3 — Datenminimierung

- Standardauflösung GPS: **50 m** (reduzierbar auf 1 km)
- Aufbewahrungsdauer: **30 Tage** (konfigurierbar)
- Anonymisierung: alle personenbezogenen Geräte-IDs werden vor Verlassen
  des Geräts mit SHA-256 gehasht.

## § 4 — Beschäftigten-Hinweise (BDSG-Nutzung)

Wird SecureGuard im Beschäftigten-Verhältnis eingesetzt, ist der Betriebs-
oder Personalrat gemäß § 26 Abs. 4 BDSG rechtzeitig einzubinden. Insbesondere:

1. Mitbestimmung bei Einführung neuer Auswertungen
2. Information über automatische Protokollierung
3. Festlegung von Auswertungs-Zwecken und Empfängern

## § 5 — Betroffenenrechte

- Auskunft (Art. 15 DSGVO) → siehe Menüpunkt **Rechte**
- Löschung (Art. 17) → konfigurierbarer Aufbewahrungs-Timer
- Widerspruch (Art. 21) → Deaktivierung in **Settings**

## § 6 — TOMs (Technisch-organisatorische Maßnahmen)

- AES-256-Verschlüsselung sensibler Felder
- TLS 1.2+ für externe HTTP-Calls
- Rollenbasiertes UI (Admin, Operator, Auditor)
- Audit-Log in `tombstone`-Partition

## § 7 — Inkrafttreten

Diese Vereinbarung tritt mit Installation der Anwendung in Kraft. Bei
Funktionsupdates mit neuer Datenverarbeitung wird eine neue Version zur
Bestätigung vorgelegt.
