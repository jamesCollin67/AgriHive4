package com.example.agrihive.addapiary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EditApiaryViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Update the apiary name and location.
     */
    fun updateApiary(apiaryId: String, name: String, location: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Apiary name cannot be empty"
            return
        }
        if (location.isBlank()) {
            _errorMessage.value = "Location cannot be empty"
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            _errorMessage.value = "Not authenticated"
            return
        }

        _isLoading.value = true

        firestore.collection("apiaries").document(apiaryId)
            .update(
                mapOf(
                    "name" to name,
                    "location" to location,
                    "lastUpdate" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                _isLoading.value = false
                _updateSuccess.value = true
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Failed to update apiary"
            }
    }

    /**
     * Permanently delete the apiary document.
     */
    fun deleteApiary(apiaryId: String) {
        _isLoading.value = true

        firestore.collection("apiaries").document(apiaryId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                _deleteSuccess.value = true
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = e.message ?: "Failed to delete apiary"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
