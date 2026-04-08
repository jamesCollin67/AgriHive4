package com.example.agrihive.addapiary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class BeeFarm(
    val id: String = "",
    val name: String = "",
    val address: String = ""
)

class AddApiaryViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _beeFarms = MutableLiveData<List<BeeFarm>>()
    val beeFarms: LiveData<List<BeeFarm>> = _beeFarms

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var saveTimedOut = false

    init {
        loadBeeFarms()
    }

    private fun loadBeeFarms() {
        // Hardcoded farms as primary source — always available offline
        val hardcodedFarms = listOf(
            BeeFarm("1", "Apis Prince Honeybee Farm", "Apis Prince Honeybee Farm, Greener's Farm, Taptap, Cebu City, Cebu"),
            BeeFarm("2", "GKG FARM CEBU PH", "Cansiguiring, Carmen, Cebu")
        )
        _beeFarms.value = hardcodedFarms

        // Also try to update from Firestore in case admin adds more farms later
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("bee_farms").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val farms = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val address = doc.getString("address") ?: return@mapNotNull null
                        BeeFarm(doc.id, name, address)
                    }
                    if (farms.isNotEmpty()) _beeFarms.value = farms
                }
            }
    }

    fun saveApiary(name: String, location: String, farmName: String, nodeId: String) {
        if (name.isBlank() || location.isBlank() || farmName.isBlank() || nodeId.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _errorMessage.value = "User not authenticated"
            return
        }

        _isLoading.value = true
        _saveSuccess.value = false
        saveTimedOut = false
        val firestore = FirebaseFirestore.getInstance()

        val timeoutRunnable = Runnable {
            if (!saveTimedOut) {
                saveTimedOut = true
                _isLoading.value = false
                _errorMessage.value = "Save timed out. Check your internet and Firestore rules."
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000)

        val apiaryId = firestore.collection("apiaries").document().id

        val apiaryData = hashMapOf(
            "id" to apiaryId,
            "name" to name,
            "location" to location,
            "farmName" to farmName,
            "nodeId" to nodeId,
            "ownerId" to uid,
            "temperature" to 0.0,
            "humidity" to 0.0,
            "moisture" to 0.0,
            "weight" to 0.0,
            "isConnected" to false,
            "lastUpdate" to System.currentTimeMillis()
        )

        firestore.collection("apiaries").document(apiaryId).set(apiaryData)
            .addOnSuccessListener {
                if (!saveTimedOut) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    _isLoading.value = false
                    _saveSuccess.value = true
                }
            }
            .addOnFailureListener { exception ->
                if (!saveTimedOut) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    _isLoading.value = false
                    _errorMessage.value = exception.message ?: "Failed to save apiary"
                }
            }
            .addOnCanceledListener {
                if (!saveTimedOut) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    _isLoading.value = false
                    _errorMessage.value = "Save cancelled. Please try again."
                }
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timeoutHandler.removeCallbacksAndMessages(null)
    }
}
