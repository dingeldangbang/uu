package de.dingeldangbang.agentmobile.network

import com.squareup.moshi.JsonClass
import de.dingeldangbang.agentmobile.security.CloudCredentials
import de.dingeldangbang.agentmobile.security.CredentialStore
import de.dingeldangbang.agentmobile.security.EndpointNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FlowRequest(
    val request: String,
    val context: List<String>,
)

@JsonClass(generateAdapter = true)
data class FlowResponse(
    val result: String? = null,
    val output: String? = null,
)

interface RemoteService {
    @GET("health")
    suspend fun health(): retrofit2.Response<okhttp3.ResponseBody>

    @POST("api/v1/runFlow")
    suspend fun runFlow(
        @Header("Authorization") authorization: String,
        @Body request: FlowRequest,
    ): retrofit2.Response<FlowResponse>
}

data class CloudResult(val text: String, val source: String)

data class CloudTestResult(val success: Boolean, val message: String)

class CloudClient(private val credentialStore: CredentialStore) {
    suspend fun run(prompt: String, context: List<String>): CloudResult = withContext(Dispatchers.IO) {
        val credentials = credentialStore.load() ?: error("Keine Cloud-Zugangsdaten gespeichert.")
        val response = service(credentials.endpoint).runFlow(
            authorization = "Bearer ${credentials.token}",
            request = FlowRequest(GuardedPayload.prompt(prompt), context.map(GuardedPayload::context)),
        )
        if (!response.isSuccessful) error("Cloud HTTP ${response.code()}")
        val body = response.body() ?: error("Leere Cloud-Antwort")
        val text = body.result ?: body.output ?: error("Cloud-Antwort enthält kein result/output")
        CloudResult(text, "cloud")
    }

    suspend fun test(): CloudTestResult = withContext(Dispatchers.IO) {
        val credentials = credentialStore.load()
            ?: return@withContext CloudTestResult(false, "Keine Zugangsdaten gespeichert.")
        runCatching {
            val response = service(credentials.endpoint).health()
            if (response.isSuccessful) CloudTestResult(true, "Health Check erfolgreich.")
            else CloudTestResult(false, "Health HTTP ${response.code()}")
        }.getOrElse { CloudTestResult(false, it.message ?: "Verbindung fehlgeschlagen.") }
    }

    private fun service(endpoint: String): RemoteService {
        val normalized = EndpointNormalizer.normalize(endpoint)
        val http = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(http)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(RemoteService::class.java)
    }
}

private object GuardedPayload {
    fun prompt(value: String) = de.dingeldangbang.agentmobile.agent.Guardrail.sanitize(value)
    fun context(value: String) = de.dingeldangbang.agentmobile.agent.Guardrail.sanitize(value)
}
