package com.example.agrihive.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.addapiary.Apiary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ViewModel for Dashboard - MVVM Architecture
 * Handles loading apiaries and calculating statistics from Firebase Realtime Database
 */
class DashboardViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

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

    private var apiaryListener: ValueEventListener? = null

    init {
        loadUserName()
        loadApiaries()
    }

    private fun loadUserName() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        
        firebaseDatabase.reference
            .child("users")
            .child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Tfgf"
                    _userName.value = firstName
                }

                override fun onCancelled(error: DatabaseError) {
                    _userName.value = "Tfgf"
                }
            })
    }

    fun loadApiaries() {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "User not logged in"
            return
        }

        _isLoading.value = true

        apiaryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isLoading.value = false
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(ApiaryDto::class.java)?.let { dto ->
                        Apiary(
                            id = child.key ?: "",
                            name = dto.name ?: "",
                            ownerId = dto.ownerId ?: "",
                            temperature = dto.temperature ?: 0.0,
                            humidity = dto.humidity ?: 0.0,
                            weight = dto.weight ?: 0.0,
                            isConnected = dto.isConnected ?: false,
                            lastUpdate = dto.lastUpdate ?: 0L
                        )
                    }
                }
                
                _apiaries.value = list
                calculateStats(list)
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
                _errorMessage.value = error.message
            }
        }

        firebaseDatabase.reference
            .child("apiaries")
            .orderByChild("ownerId")
            .equalTo(uid)
            .addValueEventListener(apiaryListener!!)
    }

    private fun calculateStats(list: List<Apiary>) {
        _totalApiaries.value = list.size
        _onlineCount.value = list.count { it.isConnected }
        
        // Thresholds for alerts (example values)
        _alertsCount.value = list.count { 
            it.temperature > 38.0 || it.temperature < 32.0 || it.humidity > 80.0 
        }
        
        // Threshold for harvest (example value: weight > 20kg)
        _harvestReadyCount.value = list.count { it.weight > 20.0 }
    }

    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Hello"
            hour < 17 -> "Hello"
            else -> "Hello"
        } // Keeping it simple like in the image "Hello, Tfgf"
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.let { listener ->
            firebaseDatabase.reference
                .child("apiaries")
                .removeEventListener(listener)
        }
    }

    private data class ApiaryDto(
        val name: String? = null,
        val ownerId: String? = null,
        val temperature: Double? = null,
        val humidity: Double? = null,
        val weight: Double? = null,
        val isConnected: Boolean? = null,
        val lastUpdate: Long? = null
    )
}
