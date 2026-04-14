package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agrihive.addapiary.Apiary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

data class WeightPoint(val timeLabel: String, val weight: Float)

class HiveStreamsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _apiaryData = MutableLiveData<Apiary?>()
    val apiaryData: LiveData<Apiary?> = _apiaryData

    private val _weightAnalytics = MutableLiveData<WeightAnalyticsData?>()
    val weightAnalytics: LiveData<WeightAnalyticsData?> = _weightAnalytics

    // Weight history for the line chart (last 7 readings)
    private val _weightHistory = MutableLiveData<List<WeightPoint>>(emptyList())
    val weightHistory: LiveData<List<WeightPoint>> = _weightHistory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var apiaryListener: ListenerRegistration? = null
    private var analyticsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null

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

                val apiary = Apiary(
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
                    // Safely handle both Timestamp and Long — getTimestamp() throws if field is a Long
                    lastUpdate = when (val raw = doc.get("lastUpdate")) {
                        is com.google.firebase.Timestamp -> raw.toDate().time
                        is Long -> raw
                        is Number -> raw.toLong()
                        else -> 0L
                    }
                )
                _apiaryData.value = apiary

                // Auto-save weight reading to history if connected
                if (apiary.isConnected && apiary.weight > 0) {
                    saveWeightReading(apiaryId, apiary.weight)
                }
            }

        fetchWeightAnalytics(apiaryId)
        listenWeightHistory(apiaryId)
    }

    /** Save current weight reading into a sub-collection for chart history */
    private fun saveWeightReading(apiaryId: String, weight: Double) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
            val label = sdf.format(java.util.Date())
            firestore.collection("apiaries").document(apiaryId)
                .collection("weight_history")
                .add(mapOf(
                    "weight" to weight,
                    "label" to label,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
        }
    }

    /** Listen to last 10 weight readings for the chart */
    private fun listenWeightHistory(apiaryId: String) {
        historyListener?.remove()
        historyListener = firestore.collection("apiaries").document(apiaryId)
            .collection("weight_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val points = snapshot.documents
                    .mapNotNull { doc ->
                        val w = doc.getDouble("weight")?.toFloat() ?: return@mapNotNull null
                        val l = doc.getString("label") ?: ""
                        WeightPoint(l, w)
                    }
                    .reversed() // oldest first for chart
                _weightHistory.value = points
            }
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
        historyListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
        analyticsListener?.remove()
        historyListener?.remove()
    }
}
