package com.example.agrihive.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.agrihive.addapiary.Apiary
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.data.local.AgriHiveDatabase
import com.example.agrihive.data.local.ApiaryEntity
import com.example.agrihive.utils.NetworkConnectivityObserver
import com.example.agrihive.utils.ConnectivityObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager = UserSessionManager(application)
    private val database = AgriHiveDatabase.getDatabase(application)
    private val connectivityObserver = NetworkConnectivityObserver(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _apiaries = MutableLiveData<List<Apiary>>()
    val apiaries: LiveData<List<Apiary>> = _apiaries

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _totalApiaries = MutableLiveData<Int>(0)
    val totalApiaries: LiveData<Int> = _totalApiaries

    private val _onlineCount = MutableLiveData<Int>(0)
    val onlineCount: LiveData<Int> = _onlineCount

    private val _alertsCount = MutableLiveData<Int>(0)
    val alertsCount: LiveData<Int> = _alertsCount

    private val _harvestReadyCount = MutableLiveData<Int>(0)
    val harvestReadyCount: LiveData<Int> = _harvestReadyCount

    private val _connectionStatus = MutableLiveData<ConnectivityObserver.Status>()
    val connectionStatus: LiveData<ConnectivityObserver.Status> = _connectionStatus

    private var apiaryListener: ListenerRegistration? = null

    init {
        observeConnection()
        loadUserName()
        loadFromCache()
        // We no longer call loadApiaries() immediately here because it relies on _connectionStatus
        // which might not have been emitted yet. The onEach in observeConnection will handle it.
    }

    private fun observeConnection() {
        connectivityObserver.observe().onEach { status ->
            _connectionStatus.postValue(status)
            if (status == ConnectivityObserver.Status.Available) {
                _errorMessage.postValue(null) // Clear "No Internet" message when connection returns
                loadApiaries()
            } else {
                _errorMessage.postValue("No Internet Connection. Showing cached data.")
            }
        }.launchIn(viewModelScope)
    }

    private fun loadFromCache() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            database.apiaryDao().getApiariesByOwner(uid).collect { cached ->
                if (cached.isNotEmpty() && _apiaries.value.isNullOrEmpty()) {
                    val list = cached.map { it.toDomain() }
                    _apiaries.postValue(list)
                    calculateStats(list)
                }
            }
        }
    }

    private fun loadUserName() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (sessionManager.hasUserData()) {
            _userName.value = sessionManager.getFirstName()
        }
        
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: "User"
                    _userName.value = firstName
                    sessionManager.saveUserData(firstName = firstName, uid = uid)
                }
            }
    }

    fun loadApiaries() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        
        // If we know for sure there's no internet, don't even try and show cached message
        if (_connectionStatus.value != null && _connectionStatus.value != ConnectivityObserver.Status.Available) {
            _errorMessage.value = "No Internet Connection. Showing cached data."
            return
        }

        _isLoading.value = true
        apiaryListener?.remove()
        apiaryListener = firestore.collection("apiaries")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    // Only show server error if we haven't already shown a connection error
                    if (_connectionStatus.value == ConnectivityObserver.Status.Available) {
                        _errorMessage.value = "Server error: ${error.message}"
                    }
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    val moisture = if (!(doc.getBoolean("isConnected") ?: false)) 0.0 else doc.getDouble("moisture") ?: 0.0
                    Apiary(
                        id = doc.id,
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
                saveToCache(list)
            }
    }

    private fun saveToCache(list: List<Apiary>) {
        viewModelScope.launch {
            val entities = list.map { it.toEntity() }
            database.apiaryDao().insertApiaries(entities)
        }
    }

    private fun calculateStats(list: List<Apiary>) {
        _totalApiaries.value = list.size
        _onlineCount.value = list.count { it.isConnected }
        _alertsCount.value = list.count { 
            it.moisture > 18.0 || (it.temperature > 0 && (it.temperature < 34.0 || it.temperature > 36.0))
        }
        _harvestReadyCount.value = list.count { it.moisture in 0.1..18.0 && it.weight > 5.0 }
    }

    private fun ApiaryEntity.toDomain() = Apiary(
        id, name, location, nodeId, ownerId, temperature, humidity, moisture, weight, isConnected, alertsCount, lastUpdate
    )

    private fun Apiary.toEntity() = ApiaryEntity(
        id, name, location, nodeId, ownerId, temperature, humidity, moisture, weight, isConnected, alertsCount, lastUpdate
    )

    fun clearError() { _errorMessage.value = null }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
    }
}
