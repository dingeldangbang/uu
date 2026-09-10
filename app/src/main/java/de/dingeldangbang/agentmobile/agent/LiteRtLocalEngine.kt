package de.dingeldangbang.agentmobile.agent

import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed interface LocalResult {
    data class Success(val text: String) : LocalResult
    data class Unavailable(val reason: String) : LocalResult
}

interface LocalInferenceEngine {
    suspend fun infer(prompt: String, context: List<String>): LocalResult
}

class LiteRtLocalEngine(private val context: Context) : LocalInferenceEngine {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private val modelFile: File
        get() = File(context.filesDir, MODEL_NAME)

    override suspend fun infer(prompt: String, context: List<String>): LocalResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!modelFile.isFile || modelFile.length() == 0L) {
                return@withContext LocalResult.Unavailable(
                    "Kein lokales Modell installiert. Importiere eine .litertlm-Datei in den Einstellungen.",
                )
            }
            runCatching {
                val activeEngine = engine ?: Engine(
                    EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath,
                    ),
                ).also {
                    it.initialize()
                    engine = it
                }
                val promptWithContext = buildString {
                    if (context.isNotEmpty()) {
                        append("Kontext:\n")
                        append(context.joinToString("\n"))
                        append("\n\n")
                    }
                    append("Anfrage: ")
                    append(prompt)
                }
                val conversation = activeEngine.createConversation()
                try {
                    LocalResult.Success(conversation.sendMessage(promptWithContext).toString())
                } finally {
                    conversation.close()
                }
            }.getOrElse { error ->
                LocalResult.Unavailable("Lokales Modell konnte nicht geladen werden: ${error.message ?: error::class.simpleName}")
            }
        }
    }

    suspend fun installModel(uri: Uri) = withContext(Dispatchers.IO) {
        val temporary = File(context.filesDir, "$MODEL_NAME.part")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Modell konnte nicht gelesen werden." }
            temporary.outputStream().use { output -> input.copyTo(output) }
        }
        require(temporary.length() > 0L) { "Die Modelldatei ist leer." }
        engine?.close()
        engine = null
        check(temporary.renameTo(modelFile)) { "Modell konnte nicht installiert werden." }
    }

    fun modelInstalled(): Boolean = modelFile.isFile && modelFile.length() > 0L

    suspend fun close() = mutex.withLock {
        engine?.close()
        engine = null
    }

    private companion object {
        const val MODEL_NAME = "agent-model.litertlm"
    }
}
