package com.secureguard.enterprise.services

import android.content.Context
import android.util.Log
import com.honeywell.aidc.BarcodeReadEvent
import com.honeywell.aidc.BarcodeReader
import com.honeywell.aidc.FailureEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Honeywell DataCollection SDK (CT45P / Android 11).
 *
 * Umfasst:
 * - `claim()` / `release()` muss zwingend aus Activity-Lifecycle
 *   (`onResume()` / `onPause()`) getrieben werden.
 * - Hardware-Trigger-Button ruft `onTriggerEvent` mit trigId=1 auf.
 *   Wir aktivieren softwaretrigger so, dass die App wie gewohnt
 *   - piept (kann pro-Scanner konfiguriert werden)
 *   - und der Listener `onBarcodeRead` answered wird.
 *
 * Felder:
 * - `scans`: SharedFlow<String>   jeweils rohe QR-/Barcode-Daten,
 *   sobald ein Scan erfolgt.
 * - `isAvailable`: true, wenn `BarcodeReader.from(ctx)` einen Reader
 *   liefern kann (= Honeywell DataCollection-Service vorhanden).
 */
@Singleton
class HoneywellScanner @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private var barcodeReader: BarcodeReader? = null
    private var claimed = false

    private val _scans = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    val scans: SharedFlow<String> = _scans.asSharedFlow()

    private val _triggered = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 8)
    val triggered: SharedFlow<Boolean> = _triggered.asSharedFlow()

    private val readerListener = object : BarcodeReader.BarcodeListener {
        override fun onBarcodeRead(event: BarcodeReadEvent) {
            val code = event.text ?: return
            Log.i(TAG, "scan → '$code'  (${event.codeId} ${event.charset})")
            _scans.tryEmit(code)
        }
        override fun onFailureEvent(event: FailureEvent) {
            Log.w(TAG, "scan failure", event.throwable)
        }
    }

    private val triggerListener = object : BarcodeReader.TriggerListener {
        override fun onTriggerEvent(event: BarcodeReader.TriggerEvent) {
            // Honeywell SDK: trigId wechselt zwischen PRESS (1) und RELEASE (0).
            // Wir leiten die UI mit einer generalisierten "true"-Emission an.
            val fired = event.trigId
            _triggered.tryEmit(true)
            Log.d(TAG, "hardware trigger fired (trigId=$fired)")
        }
    }

    /** Wie an das SDK angebunden. true wenn Honeywell-Service abrufbar. */
    fun isAvailable(): Boolean = try {
        BarcodeReader.from(ctx) != null
    } catch (e: Throwable) {
        Log.w(TAG, "Honeywell DataCollection service unavailable", e)
        false
    }

    /**
     * Soll in `Activity.onResume()` (Compose: `Lifecycle.Event.ON_RESUME`) aufgerufen werden.
     * Idempotent: mehrfaches `claim()` ist sicher.
     */
    fun claim() {
        if (claimed) return
        try {
            val reader = BarcodeReader.from(ctx) ?: run {
                Log.w(TAG, "Honeywell reader nicht verfügbar")
                return
            }
            reader.addBarcodeListener(readerListener)
            reader.addTriggerListener(triggerListener)
            reader.claim()
            barcodeReader = reader
            claimed = true
            Log.i(TAG, "Honeywell Scanner CLAIMED")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "claim failed (state)", e)
        } catch (e: Throwable) {
            Log.e(TAG, "claim failed (other)", e)
        }
    }

    /**
     * Soll in `Activity.onPause()` (Compose: `Lifecycle.Event.ON_PAUSE`)
     * aufgerufen werden, damit andere Apps den Scanner nutzen können.
     */
    fun release() {
        if (!claimed) return
        try {
            barcodeReader?.let {
                it.removeBarcodeListener(readerListener)
                it.removeTriggerListener(triggerListener)
                it.release()
            }
            barcodeReader = null
            claimed = false
            Log.i(TAG, "Honeywell Scanner RELEASED")
        } catch (e: Throwable) {
            Log.w(TAG, "release failed", e)
        }
    }

    /** software_schnittstelle für externe Trigger z.B. UI-Button. */
    fun softScan() {
        try {
            barcodeReader?.softScanOn()
            Log.d(TAG, "softScan triggered")
        } catch (e: Throwable) {
            Log.w(TAG, "softScan failed", e)
        }
    }

    fun softScanOff() {
        try {
            barcodeReader?.softScanOff()
        } catch (e: Throwable) {
            Log.w(TAG, "softScanOff failed", e)
        }
    }

    companion object { private const val TAG = "HoneywellScanner" }
}
