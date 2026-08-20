package com.secureguard.enterprise.presentation.ui.addasset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetCategory
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAssetViewModel @Inject constructor(
    private val repo: SecureGuardRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AddAssetUiState())
    val state: StateFlow<AddAssetUiState> = _state.asStateFlow()

    private lateinit var scannedPayload: ScannedPayload

    fun setScannedPayload(payload: ScannedPayload?) {
        scannedPayload = payload ?: ScannedPayload.EMPTY
        _state.update {
            it.copy(
                id      = payload?.id.orEmpty(),
                mac     = payload?.mac.orEmpty(),
                name    = payload?.name.orEmpty(),
                message = "QR-Code erkannt — bitte ergänzen & speichern."
            )
        }
    }

    fun setName(v: String)    = _state.update { it.copy(name = v) }
    fun setMac(v: String)     = _state.update { it.copy(mac = v) }
    fun setLat(v: String)     = _state.update { it.copy(latitude = v.toDoubleOrNull()) }
    fun setLon(v: String)     = _state.update { it.copy(longitude = v.toDoubleOrNull()) }
    fun setLocation(v: String)= _state.update { it.copy(location = v) }
    fun setCategory(c: AssetCategory) = _state.update { it.copy(category = c) }
    fun setBattery(v: Int)    = _state.update { it.copy(batteryPercent = v.coerceIn(0, 100)) }

    suspend fun save(): Result<Asset> {
        val s = _state.value
        if (s.id.isBlank()) return Result.failure(IllegalArgumentException("ID fehlt"))
        if (s.name.isBlank()) return Result.failure(IllegalArgumentException("Name fehlt"))

        val asset = Asset(
            id              = s.id,
            name            = s.name,
            shortName       = s.name.take(8),
            mac             = s.mac,
            category        = s.category,
            latitude        = s.latitude,
            longitude       = s.longitude,
            location        = s.location.ifBlank { null },
            batteryPercent  = s.batteryPercent,
            rssi            = -60,
            status          = AssetStatus.OFFLINE,
            lastSeen        = null,
            tags            = listOfNotNull(s.location.ifBlank { null }),
            owner           = "operator",
            externalAllowed = false
        )
        return runCatching {
            repo.upsertAsset(asset)
            asset
        }
    }
}

data class AddAssetUiState(
    val id: String = "",
    val name: String = "",
    val mac: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location: String = "",
    val batteryPercent: Int = 100,
    val category: AssetCategory = AssetCategory.GENERIC,
    val message: String = ""
)

data class ScannedPayload(
    val id: String,
    val name: String,
    val mac: String
) {
    companion object {
        val EMPTY = ScannedPayload("", "", "")

        /** Erkennt Standardformate: JSON / URL / Plain. */
        fun of(raw: String): ScannedPayload {
            val text = raw.trim()
            // JSON-Look
            if (text.startsWith("{")) {
                val id   = "id\":\"(.*?)\"".toRegex().find(text)?.groupValues?.get(1).orEmpty()
                val name = "name\":\"(.*?)\"".toRegex().find(text)?.groupValues?.get(1).orEmpty()
                val mac  = "mac\":\"(.*?)\"".toRegex().find(text)?.groupValues?.get(1).orEmpty()
                if (id.isNotBlank()) return ScannedPayload(id, name, mac)
            }
            // secureguard://asset?id=..&name=..&mac=..
            val uri = runCatching { java.net.URI(text) }.getOrNull()
            if (uri?.scheme == "secureguard") {
                val q = uri.rawQuery ?: return EMPTY
                val params = q.split("&").associate {
                    val (k, v) = it.split("=", limit = 2).let { p -> p[0] to p.getOrElse(1) { "" } }
                    k to java.net.URLDecoder.decode(v, "UTF-8")
                }
                return ScannedPayload(
                    id   = params["id"]  .orEmpty(),
                    name = params["name"] .orEmpty(),
                    mac  = params["mac"]  .orEmpty()
                )
            }
            // Plain: id\nname\nmac
            val lines = text.split("\n", limit = 3)
            if (lines.size >= 3) return ScannedPayload(lines[0], lines[1], lines[2])
            return EMPTY
        }
    }
}
