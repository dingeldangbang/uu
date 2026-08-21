package com.secureguard.enterprise.data.model

import java.util.Date

/**
 * Einheitliches Suchergebnis für alle 11 searchAsset()-Kanäle des Agenten.
 *
 * Vorher: jeder Service gab `Detection?` zurück (nullable Detection).
 * Problem: kein einheitliches Fehler-/NotFound-/Success-Schema.
 *
 * Neu: Sealed-Class-Result mit `found`-Flag, optionalem Error-Message
 *       und Detection bei gefundenem Asset.
 *
 * Kompatibilität: bestehende searchAsset(asset): Detection? bleiben — sie
 * rufen intern `build(resultOutcome, detection)` auf. Neue Methode:
 *     searchAsset(asset): SearchResult
 */
data class SearchResult(
    val found: Boolean,
    val detection: Detection? = null,
    val source: DetectionSource,
    val accuracy: Float = 0f,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        /** Erfolgreiche Suche mit Detection. */
        fun success(
            detection: Detection,
            source: DetectionSource,
            accuracy: Float = 0.8f,
            durationMs: Long = 0L,
            metadata: Map<String, Any> = emptyMap()
        ): SearchResult = SearchResult(
            found = true,
            detection = detection,
            source = source,
            accuracy = accuracy,
            durationMs = durationMs,
            metadata = metadata
        )

        /** Suche durchgeführt, aber Asset nicht gefunden (kein Fehler). */
        fun notFound(
            source: DetectionSource,
            durationMs: Long = 0L,
            metadata: Map<String, Any> = emptyMap()
        ): SearchResult = SearchResult(
            found = false,
            source = source,
            durationMs = durationMs,
            metadata = metadata
        )

        /** Suche fehlgeschlagen (Exception / Provider nicht erreichbar). */
        fun error(
            source: DetectionSource,
            error: String,
            durationMs: Long = 0L
        ): SearchResult = SearchResult(
            found = false,
            source = source,
            durationMs = durationMs,
            error = error
        )
    }
}
