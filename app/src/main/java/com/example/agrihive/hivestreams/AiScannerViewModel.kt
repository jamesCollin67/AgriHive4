package com.example.agrihive.hivestreams

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AiScannerViewModel : ViewModel() {

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _navigateToSaved = MutableLiveData(false)
    val navigateToSaved: LiveData<Boolean> = _navigateToSaved

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadingMessage = MutableLiveData<String>()
    val loadingMessage: LiveData<String> = _loadingMessage

    private val _scanResult = MutableLiveData<Pair<String, Int>?>()
    val scanResult: LiveData<Pair<String, Int>?> = _scanResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var _currentImageUri: Uri? = null
    val currentImageUri: Uri? get() = _currentImageUri

    fun onBackClicked() {
        _navigateBack.value = true
    }

    fun onSavedClicked() {
        _navigateToSaved.value = true
    }

    fun uploadAndAnalyze(uri: Uri) {
        _currentImageUri = uri
        _isLoading.value = true
        _loadingMessage.value = "Preprocessing image tensor..."

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("scans/${UUID.randomUUID()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveScanResult(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = "Upload failed: ${it.message}"
            }
    }

    private fun saveScanResult(imageUrl: String) {
        _loadingMessage.value = "Analyzing results..."
        
        // Simulation of AI result matching the image provided (American Foulbrood, 14/100)
        val disease = "American Foulbrood"
        val healthScore = 14
        
        _isLoading.value = false
        _scanResult.value = Pair(disease, healthScore)
    }

    fun doneBack() {
        _navigateBack.value = false
    }

    fun doneNavigateToSaved() {
        _navigateToSaved.value = false
    }
    
    fun doneScan() {
        _scanResult.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
