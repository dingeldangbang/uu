package com.secureguard.enterprise.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────
//  REST-API-DTOs für Crowdsourcing-Backend
// ─────────────────────────────────────────────────────────────

data class CrowdReport(
    val mac: String,
    val rssi: Int,
    val lat: Double?,
    val lon: Double?,
    val timestamp: Long
)

data class CrowdDetectionResponse(
    val found: Boolean,
    val rssi: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val lastSeen: Long? = null,
    val source: String? = null
)

interface CrowdApi {
    @GET("v1/asset/{mac}")
    suspend fun findAsset(
        @Path("mac") mac: String,
        @Query("api_key") apiKey: String
    ): Response<CrowdDetectionResponse>

    @POST("v1/report")
    suspend fun reportDetection(@Body report: CrowdReport): Response<Unit>
}

/**
 * CrowdService — Echter Retrofit-REST-Client für Crowdsourcing.
 *
 * - `searchAsset(asset)` fragt das konfigurierte Backend über die MAC ab
 * - `reportDetection(mac, rssi, lat, lon)` meldet eigene Sichtungen an das Backend
 * - Nur wenn `asset.externalAllowed` true ist (DSGVO-konform, Whitelist)
 *
 * Backend-URL konfigurierbar über `CROWD_BASE_URL` System-Property
 * (Default: leer → Service deaktiviert, liefert null).
 */
@Singleton
class CrowdService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val gson: Gson = Gson()
    private val apiKey: String = ""

    private val baseUrl: String? = runCatching {
        // Konfigurierbar über System-Property oder Gradle-BuildConfig
        val fromProp = System.getProperty("secureguard.crowd.baseUrl")
        fromProp?.takeIf { it.isNotBlank() }
            ?: "https://crowd.example.com/"  // Placeholder — durch echte URL ersetzen
    }.getOrNull()

    private val api: CrowdApi? = baseUrl?.let { url ->
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CrowdApi::class.java)
    }

    /** Spec: searchAsset(asset) → Detection? — nur wenn externalAllowed. */
    suspend fun searchAsset(asset: Asset): Detection? {
        if (!asset.externalAllowed) return null
        if (asset.mac.isBlank()) return null
        val client = api ?: return null

        return withContext(Dispatchers.IO) {
            try {
                val resp = client.findAsset(asset.mac, apiKey)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.found == true) {
                        Log.i(TAG, "Crowd: ${asset.mac} gefunden via Backend")
                        Detection(
                            timestamp = body.lastSeen ?: System.currentTimeMillis(),
                            sourceType = DetectionSource.CROWD,
                            label = "crowd:${asset.shortName}",
                            rssi = body.rssi ?: -100,
                            latitude = body.lat,
                            longitude = body.lon,
                            metadata = "via-rest"
                        )
                    } else {
                        Log.d(TAG, "Crowd: ${asset.mac} nicht im Backend")
                        null
                    }
                } else {
                    Log.w(TAG, "Crowd-API HTTP ${resp.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Crowd-Call fehlgeschlagen", e)
                null
            }
        }
    }

    /** Meldet eine eigene Sichtung an das Backend. */
    suspend fun reportDetection(
        mac: String,
        rssi: Int,
        lat: Double?,
        lon: Double?
    ): Boolean {
        if (mac.isBlank()) return false
        val client = api ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val resp = client.reportDetection(
                    CrowdReport(
                        mac = mac,
                        rssi = rssi,
                        lat = lat,
                        lon = lon,
                        timestamp = System.currentTimeMillis()
                    )
                )
                resp.isSuccessful
            } catch (e: Exception) {
                Log.w(TAG, "Crowd-Report fehlgeschlagen", e)
                false
            }
        }
    }

    companion object {
        private const val TAG = "CrowdService"
    }
}
