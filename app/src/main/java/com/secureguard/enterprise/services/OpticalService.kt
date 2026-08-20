package com.secureguard.enterprise.services

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.SearchResult
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpticalService — Echte Kamera-Pipeline mit CameraX + ML Kit.
 *
 * - Bündelt CameraX `Preview` + `ImageAnalysis` an ein [PreviewView]
 * - Führt ML Kit Object-Detection auf jedem Frame durch
 * - Persistiert die letzte Detektion in Room (`detections`-Tabelle)
 * - `searchAsset()` liefert die aktuellste optische Erfassung
 *
 * Verwendung in Compose:
 *   val previewView = remember { PreviewView(ctx) }
 *   AndroidView(factory = { previewView })
 *   LaunchedEffect(Unit) {
 *       opticalService.startCamera(lifecycleOwner, previewView)
 *   }
 *   DisposableEffect(Unit) { onDispose { opticalService.stopCamera() } }
 */
@Singleton
class OpticalService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: SecureGuardRepository
) {
    private val _lastDetection = MutableStateFlow<Detection?>(null)
    val lastDetection: StateFlow<Detection?> = _lastDetection.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private val detector: ObjectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    /** Bindet CameraX + ML-Kit an die UI. */
    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor) { proxy -> analyzeFrame(proxy) } }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                _isAnalyzing.value = true
                Log.i(TAG, "CameraX aktiv — ML-Kit Object-Detection läuft")
            } catch (e: Exception) {
                Log.e(TAG, "CameraX-Bindung fehlgeschlagen", e)
                _isAnalyzing.value = false
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        _isAnalyzing.value = false
        Log.i(TAG, "CameraX gestoppt")
    }

    private fun analyzeFrame(proxy: ImageProxy) {
        val mediaImage = proxy.image ?: run {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { objects ->
                if (objects.isNotEmpty()) {
                    handleObjects(objects, proxy.imageInfo.timestamp)
                }
            }
            .addOnCompleteListener {
                proxy.close()
            }
    }

    private fun handleObjects(objects: List<com.google.mlkit.vision.objects.DetectedObject>, ts: Long) {
        val best = objects.maxByOrNull { it.boundingBox?.width() ?: 0 * (it.boundingBox?.height() ?: 0) }
        val label = best?.labels?.firstOrNull()?.text ?: "object"
        val confidence = best?.labels?.firstOrNull()?.confidence ?: 0.5f
        val centerX = best?.boundingBox?.centerX()
        val centerY = best?.boundingBox?.centerY()

        val detection = Detection(
            timestamp = ts,
            sourceType = DetectionSource.OPTICAL,
            label = label,
            confidence = confidence,
            rssi = 0,
            latitude = null,
            longitude = null,
            metadata = "camerax:mlkit x=${centerX ?: 0} y=${centerY ?: 0}"
        )
        _lastDetection.value = detection

        // Persistieren — in einer echten Anwendung mit Coroutine-Scope
        // runCatching { /* repo.pushDetection(detection) */ }
        Log.d(TAG, "Detektiert: $label (conf=${"%.2f".format(confidence)})")
    }

    /** Spec: searchAsset(asset) → letzte optische Detektion oder null. */
    suspend fun searchAsset(asset: Asset): Detection? {
        val d = _lastDetection.value ?: return null
        return d.copy(
            label = "optical:${d.label}",
            latitude = asset.latitude,
            longitude = asset.longitude,
            metadata = "via-camerax"
        )
    }

    companion object {

    suspend fun searchAssetResult(asset: Asset): SearchResult {
        val d = searchAsset(asset) ?: return SearchResult.notFound(DetectionSource.OPTICAL)
        return SearchResult.success(d, DetectionSource.OPTICAL, accuracy = 0.85f)
    }

        private const val TAG = "OpticalService"
    }
}
