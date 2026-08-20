package com.secureguard.enterprise.services.apis

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ─────────────────────────────────────────────────────────────
//  DTOs
// ─────────────────────────────────────────────────────────────

data class WiGleResult(val results: List<WiGleBssid> = emptyList())
data class WiGleBssid(val bssid: String = "", val trilat: Double? = null, val trilong: Double? = null)

data class MacLookupResponse(val success: Boolean = false, val found: Boolean = false, val vendor: String? = null)

data class OpenChargeStation(
    val id: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val operatorInfo: OperatorInfo? = null,
    val statusType: StatusType? = null
)
data class OperatorInfo(val title: String? = null)
data class StatusType(val isOperational: Boolean = false)

data class DhlStation(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class CkanResponse(val result: CkanResult = CkanResult())
data class CkanResult(val results: List<CkanDataset> = emptyList())
data class CkanDataset(val id: String = "", val title: String = "")

data class GeolocationRequest(val wifiAccessPoints: List<WifiAccessPoint> = emptyList())
data class WifiAccessPoint(val macAddress: String, val signalStrength: Int = 0)
data class GeolocationResponse(val location: GeoLocation = GeoLocation(), val accuracy: Double = 0.0)
data class GeoLocation(val lat: Double = 0.0, val lng: Double = 0.0)

data class NetatmoResponse(val body: NetatmoBody = NetatmoBody())
data class NetatmoBody(val devices: List<NetatmoDevice> = emptyList())
data class NetatmoDevice(
    val _id: String = "",
    val place: NetatmoPlace = NetatmoPlace(),
    val dashboard_data: NetatmoDashboard = NetatmoDashboard()
)
data class NetatmoPlace(val latitude: Double = 0.0, val longitude: Double = 0.0)
data class NetatmoDashboard(val Temperature: Double = 0.0, val Humidity: Double = 0.0)

data class HeliumResponse(val data: List<HeliumHotspot> = emptyList())
data class HeliumHotspot(
    val id: String = "",
    val name: String = "",
    val status: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

// ─────────────────────────────────────────────────────────────
//  Retrofit-Interfaces
// ─────────────────────────────────────────────────────────────

interface WiGleApi {
    @GET("api/v1/network/detail")
    suspend fun searchBssid(@Query("netid") bssid: String): Response<WiGleResult>
}

interface MacLookupApi {
    @GET("v2/macs/{mac}")
    suspend fun lookupMac(@Path("mac") mac: String): Response<MacLookupResponse>
}

interface OpenChargeMapApi {
    @GET("poi")
    suspend fun getStations(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("maxresults") maxResults: Int = 5
    ): Response<List<OpenChargeStation>>
}

interface DhlPackstationApi {
    @GET("location-finder/v1/find-by-address")
    suspend fun getPackstations(
        @Query("address") address: String = "",
        @Query("countryCode") country: String = "DE",
        @Query("radius") radius: Int = 2000
    ): Response<List<DhlStation>>
}

interface CKANOpenDataApi {
    @GET("api/3/action/package_search")
    suspend fun searchDatasets(@Query("q") query: String): Response<CkanResponse>
}

interface GoogleGeolocationApi {
    @POST("geolocate")
    suspend fun geolocate(@Body request: GeolocationRequest): Response<GeolocationResponse>
}

interface NetatmoWeatherApi {
    @GET("api/getstationsdata")
    suspend fun getStations(@Query("access_token") accessToken: String): Response<NetatmoResponse>
}

interface HeliumNetworkApi {
    @GET("v1/hotspots/lat/{lat}/lon/{lon}")
    suspend fun getHotspots(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double
    ): Response<HeliumResponse>
}
