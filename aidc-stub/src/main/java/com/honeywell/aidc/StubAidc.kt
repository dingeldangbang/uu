package com.honeywell.aidc

import android.content.Context

/**
 * ─────────────────────────────────────────────────────────────
 *  BUILD-ZEIT-STUB für das Honeywell DataCollection SDK (CT45P).
 * ─────────────────────────────────────────────────────────────
 *
 *  Das echte SDK wird über das Honeywell Tech-Portal als AAR
 *  verteilt (nicht in öffentlichen Maven-Repos). Dieses Modul
 *  stellt die von der App verwendete API-Oberfläche bereit, damit
 *  das Projekt ohne SDK baut:
 *
 *    • [BarcodeReader.from] liefert immer `null`
 *      → `HoneywellScanner.isAvailable()` meldet ehrlich "false",
 *        die App läuft auf Nicht-Honeywell-Geräten stabil.
 *    • Listener-Methoden sind No-Ops.
 *
 *  Echtes SDK einbinden: AAR nach `app/libs/` legen und in
 *  `app/build.gradle` die Abhängigkeit von `project(':aidc-stub')`
 *  auf `fileTree(dir: 'libs', include: ['*.aar'])` umstellen.
 *  Siehe auch: honeywell-experimental.yml (CI-Check).
 */

/** Barcode-/QR-Leseereignis (Stub: immer leer). */
class BarcodeReadEvent(
    val text: String? = null,
    val codeId: String? = null,
    val charset: String? = null,
    val aimId: String? = null,
    val timestamp: String? = null
) {
    override fun toString(): String = "BarcodeReadEvent(text=$text, codeId=$codeId)"
}

/** Fehlerereignis des Scanners (Stub). */
class FailureEvent(
    val throwable: Throwable = Throwable("Honeywell DataCollection SDK nicht eingebunden (aidc-stub)")
)

/**
 * Stub des Honeywell [BarcodeReader].
 *
 * `from(ctx)` liefert immer `null` — ohne echtes SDK gibt es keinen
 * Reader. Alle Methoden sind bewusst idempotente No-Ops, damit
 * `HoneywellScanner` den vollständigen Lebenszyklus (claim/release)
 * ohne Sonderfälle durchlaufen kann.
 */
class BarcodeReader private constructor() {

    /** Listener für Lese- und Fehlerereignisse. */
    interface BarcodeListener {
        fun onBarcodeRead(event: BarcodeReadEvent)
        fun onFailureEvent(event: FailureEvent)
    }

    /** Listener für Hardware-Trigger-Ereignisse. */
    interface TriggerListener {
        fun onTriggerEvent(event: TriggerEvent)
    }

    /** Trigger-Ereignis (trigId: PRESS = 1, RELEASE = 0 beim echten SDK). */
    class TriggerEvent(val trigId: Int = 1)

    companion object {
        /** Echtes SDK liefert hier den Reader; der Stub liefert `null`. */
        @JvmStatic
        fun from(context: Context): BarcodeReader? = null
    }

    fun addBarcodeListener(listener: BarcodeListener) = Unit
    fun removeBarcodeListener(listener: BarcodeListener) = Unit
    fun addTriggerListener(listener: TriggerListener) = Unit
    fun removeTriggerListener(listener: TriggerListener) = Unit

    fun claim() = Unit
    fun release() = Unit

    fun softScanOn() = Unit
    fun softScanOff() = Unit
}
