package com.secureguard.enterprise.ui.optical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LifecycleOwner
import androidx.camera.view.PreviewView
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.services.OpticalService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OpticalScanViewModel @Inject constructor(
    private val optical: OpticalService,
    private val repo: SecureGuardRepository
) : ViewModel() {

    val lastDetection: StateFlow<Detection?> = optical.lastDetection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isAnalyzing: StateFlow<Boolean> = optical.isAnalyzing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun startCamera(owner: LifecycleOwner, view: PreviewView) {
        optical.startCamera(owner, view)
    }

    fun stopCamera() {
        optical.stopCamera()
    }

    /** Persistiert die letzte Detektion in Room. */
    fun persistLastDetection() {
        viewModelScope.launch {
            val d = optical.lastDetection.value ?: return@launch
            repo.pushDetection(d.copy(sourceType = DetectionSource.OPTICAL))
        }
    }
}
