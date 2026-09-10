package de.dingeldangbang.agentmobile.agent

import android.content.Context
import de.dingeldangbang.agentmobile.data.AgentDatabase
import de.dingeldangbang.agentmobile.data.DocumentEntity
import de.dingeldangbang.agentmobile.data.HistoryEntity
import de.dingeldangbang.agentmobile.network.CloudClient
import de.dingeldangbang.agentmobile.network.NetworkUtils
import de.dingeldangbang.agentmobile.security.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class AgentResult(
    val text: String,
    val source: String,
)

class AgentRepository(
    private val database: AgentDatabase,
    private val credentialStore: CredentialStore,
    private val cloudClient: CloudClient,
    private val localEngine: LocalInferenceEngine,
    private val context: Context,
) {
    suspend fun handle(prompt: String): AgentResult = withContext(Dispatchers.IO) {
        val safePrompt = Guardrail.sanitize(prompt.trim())
        require(safePrompt.isNotBlank()) { "Die Anfrage darf nicht leer sein." }
        val documents = database.documentDao().search(safePrompt).map { "${it.title}: ${it.content}" }
        val complexity = CapabilityRouter.classify(safePrompt, documents.isNotEmpty())
        val online = NetworkUtils.isOnline(context)
        val hasCloud = credentialStore.load() != null

        val result = if (online && hasCloud && complexity != TaskComplexity.LOCAL_ONLY) {
            runCatching { cloudClient.run(safePrompt, documents) }
                .getOrElse { fallback(safePrompt, documents, "local_fallback") }
        } else {
            fallback(safePrompt, documents, if (online) "local" else "local_offline")
        }

        val guarded = AgentResult(Guardrail.sanitize(result.text), result.source)
        database.historyDao().insert(
            HistoryEntity(prompt = safePrompt, result = guarded.text, source = guarded.source),
        )
        guarded
    }

    suspend fun indexDocuments(documents: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        database.documentDao().insertAll(
            documents.map { (title, content) ->
                DocumentEntity(
                    id = title.lowercase().hashCode().toString(),
                    title = title,
                    content = Guardrail.sanitize(content),
                )
            },
        )
    }

    private suspend fun fallback(prompt: String, context: List<String>, source: String): AgentResult {
        return when (val local = localEngine.infer(prompt, context)) {
            is LocalResult.Success -> AgentResult(local.text, source)
            is LocalResult.Unavailable -> AgentResult(
                "Lokaler Fallback aktiv. ${local.reason}\n\nAnfrage gespeichert: ${prompt.take(240)}",
                source,
            )
        }
    }
}
