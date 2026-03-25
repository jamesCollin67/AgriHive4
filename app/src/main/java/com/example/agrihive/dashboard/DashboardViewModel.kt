package com.example.agrihive.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.agrihive.addapiary.Apiary
import com.example.agrihive.data.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * ViewModel for Dashboard - MVVM Architecture
 * Handles loading apiaries and calculating statistics from Firebase Realtime Database
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager = UserSessionManager(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _apiaries = MutableLiveData<List<Apiary>>()
    val apiaries: LiveData<List<Apiary>> = _apiaries

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    // Dashboard Statistics
    private val _totalApiaries = MutableLiveData<Int>(0)
    val totalApiaries: LiveData<Int> = _totalApiaries

    private val _onlineCount = MutableLiveData<Int>(0)
    val onlineCount: LiveData<Int> = _onlineCount

    private val _alertsCount = MutableLiveData<Int>(0)
    val alertsCount: LiveData<Int> = _alertsCount

    private val _harvestReadyCount = MutableLiveData<Int>(0)
    val harvestReadyCount: LiveData<Int> = _harvestReadyCount

    private var apiaryListener: ListenerRegistration? = null

    init {
        loadUserName()
        loadApiaries()
    }

    private fun loadUserName() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        
        // Initial load from session manager for instant display
        if (sessionManager.hasUserData()) {
            _userName.value = sessionManager.getFirstName()
        } else {
            _userName.value = "User"
        }
        
        // Fetch user data from Firestore to ensure latest data and sync session
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: "User"
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    
                    _userName.value = firstName
                    
                    // Update session manager
                    sessionManager.saveUserData(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        uid = uid
                    )
                }
            }
    }

    fun loadApiaries() {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "User not logged in"
            return
        }

        _isLoading.value = true
        apiaryListener?.remove()
        apiaryListener = firestore.collection("apiaries")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    _errorMessage.value = error.message ?: "Failed to load apiaries"
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    val moisture = if (!doc.getBoolean("isConnected")!!) 0.0 else doc.getDouble("moisture") ?: 0.0
                    Apiary(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        location = doc.getString("location") ?: "",
                        nodeId = doc.getString("nodeId") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        temperature = doc.getDouble("temperature") ?: 0.0,
                        humidity = doc.getDouble("humidity") ?: 0.0,
                        moisture = moisture,
                        weight = doc.getDouble("weight") ?: 0.0,
                        isConnected = doc.getBoolean("isConnected") ?: false,
                        alertsCount = (doc.getLong("alertsCount") ?: 0L).toInt(),
                        lastUpdate = doc.getLong("lastUpdate") ?: 0L
                    )
                } ?: emptyList()

                _apiaries.value = list
                calculateStats(list)
            }
    }

    private fun calculateStats(list: List<Apiary>) {
        _totalApiaries.value = list.size
        _onlineCount.value = list.count { it.isConnected }
        
        // Thresholds for alerts
        _alertsCount.value = list.count { 
            // 1. Moisture alert (> 18%)
            it.moisture > 18.0 || 
            // 2. Temp alert (Optimal: 34-36)
            (it.temperature > 0 && (it.temperature < 34.0 || it.temperature > 36.0))
        }
        
        // 3. Harvest Ready (Moisture <= 18% AND Weight stable/high)
        // Simplified: Moisture <= 18% and weight > 0
        _harvestReadyCount.value = list.count { it.moisture in 0.1..18.0 && it.weight > 5.0 }
    }

    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Hello"
            hour < 17 -> "Hello"
            else -> "Hello"
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
    }
}
