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
import com.google.firebase.firestore.FieldValue
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
                val userId = auth.currentUser?.uid ?: return@launch

                // 1. Save to local Room database
                val reportLocal = ReportEntity(
                    description = description,
                    timestamp = System.currentTimeMillis(),
                    imageUri = selectedImageUri
                )
                database.reportDao().insertReport(reportLocal)

                // 2. Save to Firestore — use serverTimestamp() so ordering works correctly
                val reportFirestore = hashMapOf(
                    "userId" to userId,
                    "name" to "${sessionManager.getFirstName()} ${sessionManager.getLastName()}".trim(),
                    "farm" to sessionManager.getFarm(),
                    "description" to description,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "imageUri" to selectedImageUri,
                    "status" to "pending",
                    "unread" to true,
                    "reply" to null,
                    "notified" to true // true = no reply yet, nothing to notify about
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
