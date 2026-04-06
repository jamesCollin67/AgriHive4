package com.example.agrihive.hivestreams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.agrihive.data.local.AgriHiveDatabase
import com.example.agrihive.data.local.ReportEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SendReportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AgriHiveDatabase.getDatabase(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _navigateToReportSent = MutableLiveData(false)
    val navigateToReportSent: LiveData<Boolean> = _navigateToReportSent

    var selectedImageUri: String? = null

    fun onSubmitClicked(description: String) {
        if (description.isBlank()) {
            _errorMessage.value = "Please describe the issue"
            return
        }

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val userId = auth.currentUser?.uid ?: "anonymous"

                // 1. Save to local Room database
                val reportLocal = ReportEntity(
                    description = description,
                    timestamp = timestamp,
                    imageUri = selectedImageUri
                )
                database.reportDao().insertReport(reportLocal)

                // 2. Save to Firestore
                val reportFirestore = hashMapOf(
                    "userId" to userId,
                    "description" to description,
                    "timestamp" to timestamp,
                    "imageUri" to selectedImageUri,
                    "status" to "pending",
                    "notified" to true // Set to true initially since there's no reply yet
                )
                
                firestore.collection("reports")
                    .add(reportFirestore)
                    .await()

                _navigateToReportSent.postValue(true)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to submit report: ${e.message}")
            }
        }
    }

    fun doneReportSentNavigation() {
        _navigateToReportSent.value = false
    }

    fun doneError() {
        _errorMessage.value = null
    }
}
