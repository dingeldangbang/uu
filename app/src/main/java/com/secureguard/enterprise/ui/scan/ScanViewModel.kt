package com.secureguard.enterprise.ui.scan

import androidx.lifecycle.ViewModel
import com.secureguard.enterprise.presentation.ui.addasset.ScannedPayload
import com.secureguard.enterprise.services.HoneywellScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanner: HoneywellScanner
) : ViewModel() {
    /** Rohscans (vollständige Barcode-Strings). */
    val scans: SharedFlow<String> = scanner.scans
    val triggered: SharedFlow<Boolean> = scanner.triggered

    fun available(): Boolean = scanner.isAvailable()
    fun claim() = scanner.claim()
    fun release() = scanner.release()
    fun softScan() = scanner.softScan()

    /** Decode für AddAssetScreen. */
    fun parse(raw: String): ScannedPayload = ScannedPayload.of(raw)
}
