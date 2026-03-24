package com.example.agrihive.addapiary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddApiaryViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun saveApiary(name: String, location: String, nodeId: String) {
        if (name.isBlank() || location.isBlank() || nodeId.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _errorMessage.value = "User not authenticated"
            return
        }

        _isLoading.value = true
        val database = FirebaseDatabase.getInstance().reference
        val apiaryId = database.child("apiaries").push().key ?: run {
            _isLoading.value = false
            _errorMessage.value = "Database error"
            return
        }

        val apiaryData = mapOf(
            "id" to apiaryId,
            "name" to name,
            "location" to location,
            "nodeId" to nodeId,
            "ownerId" to uid,
            "temperature" to 0.0,
            "humidity" to 0.0,
            "weight" to 0.0,
            "isConnected" to false,
            "lastUpdate" to System.currentTimeMillis()
        )

        database.child("apiaries").child(apiaryId).setValue(apiaryData)
            .addOnSuccessListener {
                _isLoading.value = false
                _saveSuccess.value = true
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = it.message ?: "Failed to save apiary"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
