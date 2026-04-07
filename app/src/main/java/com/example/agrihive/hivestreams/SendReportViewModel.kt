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
    private val sessionManager = com.example.agrihive.data.UserSessionManager(application)

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
                val userId = auth.currentUser?.uid ?: return@launch // Ensure user is logged in

                // 1. Save to local Room database
                val reportLocal = ReportEntity(
                    description = description,
                    timestamp = timestamp,
                    imageUri = selectedImageUri
                )
                database.reportDao().insertReport(reportLocal)

                // 2. Save to Firestore under the global collection
                // We use the userId field to ensure the admin knows who sent it
                // and the mobile app can filter its own reports.
                // Include user name and farm from session for the admin dashboard
                val reportFirestore = hashMapOf(
                    "userId" to userId,
                    "name" to "${sessionManager.getFirstName()} ${sessionManager.getLastName()}".trim(),
                    "farm" to sessionManager.getFarm(),
                    "description" to description,
                    "timestamp" to timestamp,
                    "imageUri" to selectedImageUri,
                    "status" to "pending",
                    "reply" to null,
                    "notified" to true // Initially true because there's no reply yet
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
