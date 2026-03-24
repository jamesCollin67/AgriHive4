package com.example.agrihive.camera

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class CameraViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _scanResult = MutableLiveData<String?>()
    val scanResult: LiveData<String?> = _scanResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun uploadAndAnalyze(uri: Uri, apiaryId: String) {
        _isLoading.value = true

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("scans/${UUID.randomUUID()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveScanResult(downloadUri.toString(), apiaryId)
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = "Upload failed: ${it.message}"
            }
    }

    private fun saveScanResult(imageUrl: String, apiaryId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val scanId = database.child("disease_scans").child(apiaryId).push().key ?: return
        
        // Simulation of AI result for now (as requested for backend flow)
        val result = mapOf(
            "imageUrl" to imageUrl,
            "result" to "Healthy",
            "confidence" to 0.98,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("disease_scans").child(apiaryId).child(scanId).setValue(result)
            .addOnSuccessListener {
                _isLoading.value = false
                _scanResult.value = "Healthy"
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = "Failed to save result: ${it.message}"
            }
    }
}
