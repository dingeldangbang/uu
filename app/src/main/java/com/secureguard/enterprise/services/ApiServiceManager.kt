package com.secureguard.enterprise.services

import com.secureguard.enterprise.BuildConfig
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.services.apis.CKANOpenDataApi
import com.secureguard.enterprise.services.apis.CkanDataset
import com.secureguard.enterprise.services.apis.CrowdApi
import com.secureguard.enterprise.services.apis.FreeCrowdProvider
import com.secureguard.enterprise.services.apis.GeolocationRequest
import com.secureguard.enterprise.services.apis.GoogleGeolocationApi
import com.secureguard.enterprise.services.apis.HeliumHotspot
import com.secureguard.enterprise.services.apis.HeliumNetworkApi
import com.secureguard.enterprise.services.apis.MacLookupApi
import com.secureguard.enterprise.services.apis.MacLookupResponse
import com.secureguard.enterprise.services.apis.NetatmoDevice
import com.secureguard.enterprise.services.apis.NetatmoWeatherApi
import com.secureguard.enterprise.services.apis.OpenChargeMapApi
import com.secureguard.enterprise.services.apis.OpenChargeStation
import com.secureguard.enterprise.services.apis.WiGleApi
import com.secureguard.enterprise.services.apis.WiGleResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApiServiceManager — zentraler Hoch-Punkt-Manager für die 8 externen
 * REST-API-Knoten des ApiNodeManager. Verwaltet nur die (potentiell
 * viele) Retrofit- und OkHttp-Client-Typen; die eigentliche
 * Verteilung/Knoten-Logik liegt in [com.secureguard.enterprise.agent.ApiNodeManager].
 *
 * → Die Mapping-Methoden (wandeln WigleResult → Detection etc.) bleiben
 *     im ApiNodeManager (wo die SearchContext-Normalisierung und
 *     DetectionSource-Zuweisung passiert).
 */
@Singleton
class ApiServiceManager @Inject constructor(
    private val repo: SecureGuardRepository
) {
    companion object {
        private const val TAG = "ApiServiceManager"
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ────────── Lazy Retrofit-Clients (8 Provider) ──────────
    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val wigle: WiGleApi by lazy { retrofit("https://api.wigle.net/").create(WiGleApi::class.java) }
    private val macLookup: MacLookupApi by lazy { retrofit("https://api.maclookup.app/").create(MacLookupApi::class.java) }
    private val openChargeMap: OpenChargeMapApi by lazy { retrofit("https://api.openchargemap.io/v3/").create(OpenChargeMapApi::class.java) }
    private val ckan: CKANOpenDataApi by lazy { retrofit("https://demo.ckan.org/").create(CKANOpenDataApi::class.java) }
    private val googleGeo: GoogleGeolocationApi by lazy { retrofit("https://www.googleapis.com/geolocation/v1/").create(GoogleGeolocationApi::class.java) }
    private val netatmo: NetatmoWeatherApi by lazy { retrofit("https://api.netatmo.com/").create(NetatmoWeatherApi::class.java) }
    private val helium: HeliumNetworkApi by lazy { retrofit("https://api.helium.io/").create(HeliumNetworkApi::class.java) }

    // ────────── Provider-Agnostische Crowd-API (4 Provider) ──────────
    private val crowdProvider: CrowdApi = FreeCrowdProvider(client, moshi)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Provider-Name (für UI-Anzeige). */
    fun providerName(): String = crowdProvider.name

    // ────────── Eigentliche API-Calls ──────────

    suspend fun searchWigle(bssid: String): WiGleResult? = runCatching {
        wigle.searchBssid(bssid = bssid.replace(":", "").replace("-", "")).body()
    }.getOrNull()

    suspend fun searchMacLookup(mac: String): MacLookupResponse? = runCatching {
        macLookup.lookupMac(mac).body()
    }.getOrNull()

    suspend fun searchOpenChargeMap(lat: Double, lon: Double, radius: Int = 1000): List<OpenChargeStation>? =
        runCatching { openChargeMap.getStations(lat, lon, radius).body() }.getOrNull()

    suspend fun searchCKAN(query: String): List<CkanDataset>? =
        runCatching { ckan.searchDatasets(query).body()?.result?.results }.getOrNull()

    suspend fun searchGoogleGeo(accessPoints: List<com.secureguard.enterprise.services.apis.WifiAccessPoint>):
        com.secureguard.enterprise.services.apis.GeolocationResponse? =
        runCatching { googleGeo.geolocate(GeolocationRequest(wifiAccessPoints = accessPoints)).body() }.getOrNull()

    suspend fun searchNetatmo(): List<NetatmoDevice>? = runCatching {
        netatmo.getStations(accessToken = "Bearer ${BuildConfig.NETATMO_TOKEN}").body()?.body?.devices
    }.getOrNull()

    suspend fun searchHelium(lat: Double, lon: Double, limit: Int = 10): List<HeliumHotspot>? =
        runCatching { helium.getHotspots(lat, lon).body()?.data }.getOrNull()

    /** Provider-agnostische CrowdAPI (ergänzt das mesh aus CrowdService). */
    suspend fun createCrowdInbox(timeoutMs: Long = 45000): String? =
        crowdProvider.createInbox(timeoutMs)?.address
}
