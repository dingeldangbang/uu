package com.secureguard.enterprise.mcp

/**
 * Provider-agnostische OTP / Magic-Link-Erkennung.
 *
 * Erweiterte Heuristik (gegen gängige Provider 2026, z.B. Apple/GitHub/Microsoft):
 *  · Alphanumerische Codes (Microsoft 8-stellig, GitHub 6-stellig, Apple 6-stellig, …)
 *  · Codes im Subject
 *  · Codes in markierten Context-Elementen ("verification code", "otp", "code is")
 *  · Magic-Links (https://...-link... – bevorzugt vor OTP wenn beide vorhanden)
 *  · Bevorzugung mehrerer möglicher Codes (längere spezifische vor kürzeren generischen)
 */
object OTPDetector {

    /** Erkanntes Element. */
    sealed class Extracted {
        abstract val confidence: Float

        data class OTP(
            val code: String,
            override val confidence: Float,
            val fromSubject: Boolean
        ) : Extracted()

        data class MagicLink(
            val url: String,
            override val confidence: Float
        ) : Extracted()

        data object None : Extracted() { override val confidence = 0f }
    }

    /** Heuristik in Reihenfolge zunehmender Spezifität. */
    fun extract(subject: String, body: String): Extracted {
        // 1) Magic-Link zuerst (typischerweise der primäre Verifikations-Mechanismus)
        //    z.B. https://github.com/..., https://accounts.google.com/...
        val magicLink = MAGIC_LINK_REGEX.find(body)
        if (magicLink != null) {
            val url = magicLink.value
            // Höchste Konfidenz, wenn in <a href="..."> </a>
            val inAnchor = body.contains("href=\"$url\"") || body.contains("href='$url'")
            val conf = if (inAnchor) 0.95f else 0.7f
            return Extracted.MagicLink(url, conf)
        }

        val text = "$subject\n$body"

        // 2) Code in markierten Schlüssel-Phrasen (höchste Konfidenz)
        PRIVILEGED_KEYWORDS.forEach { keyword ->
            val matcher = Regex("""$keyword[^a-z0-9]*([a-z0-9]{${'$'}{S}})(?!_)""")
            matcher.find(text)?.let { match ->
                val code = match.groupValues[1]
                if (code.length in 4..10) {
                    return Extracted.OTP(code, 0.95f, fromSubject = false)
                }
            }
        }

        // 3) Numerische Codes (klassisch)
        //    Reihenfolge: längere zuerst (Microsoft häufig 6-stellig, GitHub 6-stellig)
        listOf(8, 7, 6, 5, 4).forEach { len ->
            val m = Regex("\\b(\\d{$len})\\b").find(text)
            if (m != null) {
                val inSubject = text.indexOf(m.value) < subject.length
                val conf = if (len in 6..8) 0.85f else 0.6f
                return Extracted.OTP(m.value, conf, fromSubject = inSubject)
            }
        }

        // 4) Alphanumerische Codes (z.B. Apple Verifications-Mails, Twitter/Slack)
        //    Häufig: 6–8 Zeichen, gemischt Buchstaben+Ziffern, Word-Char-Boundary
        ALNUM_REGEX.find(text)?.let { match ->
            val code = match.value
            if (code.length in 5..8 && code.any { it.isDigit() } && code.any { it.isLetter() }) {
                return Extracted.OTP(code, 0.75f, fromSubject = false)
            }
        }

        return Extracted.None
    }

    // ── Regex-Konstanten ─────────────────────────────────
    private val MAGIC_LINK_REGEX = Regex(
        "(?:https?://|https?:/)?[A-Za-z0-9.\\-]+(?:[:/][A-Za-z0-9\\-%.?&=_~#]+)+",
        RegexOption.IGNORE_CASE
    )

    private val ALNUM_REGEX = Regex("\\b[A-Za-z0-9]{5,8}\\b")

    /** Schlüssel-Phrasen, denen wir höher vertrauen. */
    private val PRIVILEGED_KEYWORDS = listOf(
        """"code"\s*:\s*"?"""                // "code": "..." oder "code":"..."
        """"code"\s*is\s*"?"""                  // code is: ...
        """"otp"\s*:\s*"?"""                  // json / inline
        """"otp"\s*is\s*"?"""                  // otp is: xxxxx
        """verification\s+code\s*[:\-=]?\s*"?"""   // verification code:xxxxx
        """code\s+de\s+vérification\s*[:\-=]?\s*"?""" // FR
        """code\s+de\s+verificación\s*[:\-=]?\s*"?""" // ES
    )
}
