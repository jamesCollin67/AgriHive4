package com.example.agrihive.camera

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * CameraViewModel — legacy class kept for compatibility.
 *
 * The primary AI scan flow now goes through AiScannerActivity + AiScannerViewModel
 * which uses the on-device TFLite model directly (no Firebase Storage or RTDB needed).
 *
 * This ViewModel is retained only if CameraActivity is used for non-AI photo capture.
 * The old uploadAndAnalyze() flow (Firebase Storage → RTDB) has been removed as it
 * conflicted with the AiScanner flow and used Firebase services not available on Spark plan.
 */
class CameraViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigateToAiScanner = MutableLiveData<Pair<Uri, String>?>()
    val navigateToAiScanner: LiveData<Pair<Uri, String>?> = _navigateToAiScanner

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Routes the captured image to the AiScannerActivity for on-device TFLite inference.
     * @param uri The image URI captured by the camera
     * @param apiaryId The apiary this scan belongs to
     */
    fun analyzeWithAiScanner(uri: Uri, apiaryId: String) {
        _navigateToAiScanner.value = Pair(uri, apiaryId)
    }

    fun doneNavigating() {
        _navigateToAiScanner.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
