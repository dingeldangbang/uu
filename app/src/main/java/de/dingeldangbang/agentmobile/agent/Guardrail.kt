package de.dingeldangbang.agentmobile.agent

object Guardrail {
    private const val MAX_CHARS = 8_000
    private val secretPattern = Regex(
        "(?i)(api[_-]?key|access[_-]?token|password|secret|bearer)\\s*[:=]\\s*\\S+",
    )

    fun sanitize(value: String): String {
        val redacted = value.replace(secretPattern, "[REDACTED]")
        return if (redacted.length <= MAX_CHARS) redacted
        else redacted.take(MAX_CHARS) + "\n\n[TRUNCATED]"
    }
}

enum class TaskComplexity { LOCAL_ONLY, CLOUD_PREFERRED, HYBRID }

object CapabilityRouter {
    fun classify(prompt: String, hasContext: Boolean): TaskComplexity {
        val normalized = prompt.lowercase()
        if (normalized.contains(Regex("bild|video|cloud|server|online|gpt|claude"))) {
            return TaskComplexity.CLOUD_PREFERRED
        }
        if (hasContext && prompt.length < 1_000) return TaskComplexity.LOCAL_ONLY
        return TaskComplexity.HYBRID
    }
}
