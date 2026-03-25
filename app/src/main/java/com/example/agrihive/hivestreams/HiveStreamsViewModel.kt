package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.addapiary.Apiary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HiveStreamsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _apiaryData = MutableLiveData<Apiary?>()
    val apiaryData: LiveData<Apiary?> = _apiaryData

    private val _weightAnalytics = MutableLiveData<WeightAnalyticsData?>()
    val weightAnalytics: LiveData<WeightAnalyticsData?> = _weightAnalytics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var apiaryListener: ListenerRegistration? = null
    private var analyticsListener: ListenerRegistration? = null

    fun startListening(apiaryId: String) {
        _isLoading.value = true
        apiaryListener?.remove()
        apiaryListener = firestore.collection("apiaries").document(apiaryId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    _errorMessage.value = error.message ?: "Failed to load apiary data"
                    return@addSnapshotListener
                }

                val doc = snapshot
                if (doc == null || !doc.exists()) {
                    _apiaryData.value = null
                    return@addSnapshotListener
                }

                _apiaryData.value = Apiary(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    location = doc.getString("location") ?: "",
                    nodeId = doc.getString("nodeId") ?: "",
                    ownerId = doc.getString("ownerId") ?: "",
                    temperature = doc.getDouble("temperature") ?: 0.0,
                    humidity = doc.getDouble("humidity") ?: 0.0,
                    moisture = doc.getDouble("moisture") ?: 0.0,
                    weight = doc.getDouble("weight") ?: 0.0,
                    isConnected = doc.getBoolean("isConnected") ?: false,
                    alertsCount = (doc.getLong("alertsCount") ?: 0L).toInt(),
                    lastUpdate = doc.getLong("lastUpdate") ?: 0L
                )
            }

        // Listen for weight analytics/history
        fetchWeightAnalytics(apiaryId)
    }

    private fun fetchWeightAnalytics(apiaryId: String) {
        analyticsListener?.remove()
        analyticsListener = firestore.collection("weight_analytics").document(apiaryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Failed to load analytics: ${error.message}"
                    return@addSnapshotListener
                }
                _weightAnalytics.value = snapshot?.toObject(WeightAnalyticsData::class.java)
            }
    }

    fun stopListening(apiaryId: String) {
        apiaryListener?.remove()
        analyticsListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
        analyticsListener?.remove()
    }
}
