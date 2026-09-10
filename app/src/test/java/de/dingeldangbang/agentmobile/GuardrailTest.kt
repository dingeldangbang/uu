package de.dingeldangbang.agentmobile

import de.dingeldangbang.agentmobile.agent.Guardrail
import de.dingeldangbang.agentmobile.security.EndpointNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GuardrailTest {
    @Test
    fun redactsSecretsAndLimitsLength() {
        val value = Guardrail.sanitize("api_key=top-secret")
        assertEquals("[REDACTED]", value)
    }

    @Test
    fun endpointAlwaysEndsWithSlash() {
        assertEquals("https://example.test/", EndpointNormalizer.normalize("https://example.test"))
    }

    @Test
    fun endpointRejectsHttp() {
        assertThrows(IllegalArgumentException::class.java) {
            EndpointNormalizer.normalize("http://example.test")
        }
    }
}
