package com.secureguard.enterprise.services.apis

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Baut und liefert alle externen API-Clients des API-Node-Managers.
 *
 * Basis-URLs und DTOs in [ApiInterfaces]. Jeder Client ist lazy —
 * erst beim ersten Zugriff wird der Retrofit-Builder erstellt.
 *
 * Hinweis: Die meisten APIs benötigen API-Keys/Auth (WiGle, Google, Netatmo,
 * Helium, DHL). Ohne konfigurierten Key liefern die Knoten leer/null —
 * die Architektur bleibt vollständig verkabelt.
 */
@Singleton
class NodeApiFactory @Inject constructor() {

    private val gson = Gson()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    val wigle: WiGleApi by lazy { retrofit("https://api.wigle.net/").create(WiGleApi::class.java) }
    val macLookup: MacLookupApi by lazy { retrofit("https://api.maclookup.app/").create(MacLookupApi::class.java) }
    val openChargeMap: OpenChargeMapApi by lazy { retrofit("https://api.openchargemap.io/v3/").create(OpenChargeMapApi::class.java) }
    val dhl: DhlPackstationApi by lazy { retrofit("https://api-eu.dhl.com/").create(DhlPackstationApi::class.java) }
    val ckan: CKANOpenDataApi by lazy { retrofit("https://demo.ckan.org/").create(CKANOpenDataApi::class.java) }
    val googleGeo: GoogleGeolocationApi by lazy { retrofit("https://www.googleapis.com/geolocation/v1/").create(GoogleGeolocationApi::class.java) }
    val netatmo: NetatmoWeatherApi by lazy { retrofit("https://api.netatmo.com/").create(NetatmoWeatherApi::class.java) }
    val helium: HeliumNetworkApi by lazy { retrofit("https://api.helium.io/").create(HeliumNetworkApi::class.java) }
}
