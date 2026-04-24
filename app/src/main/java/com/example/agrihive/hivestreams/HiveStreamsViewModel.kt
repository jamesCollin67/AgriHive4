package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agrihive.addapiary.Apiary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WeightPoint(val timeLabel: String, val weight: Float)

class HiveStreamsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val rtdb      = FirebaseDatabase.getInstance()

    private val _apiaryData = MutableLiveData<Apiary?>()
    val apiaryData: LiveData<Apiary?> = _apiaryData

    private val _weightAnalytics = MutableLiveData<WeightAnalyticsData?>()
    val weightAnalytics: LiveData<WeightAnalyticsData?> = _weightAnalytics

    private val _weightHistory = MutableLiveData<List<WeightPoint>>(emptyList())
    val weightHistory: LiveData<List<WeightPoint>> = _weightHistory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var apiaryListener: ListenerRegistration? = null
    private var analyticsListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var rtdbListener: ValueEventListener? = null
    private var rtdbNodePath: String? = null

    // Offline timeout — marks offline if no live update arrives within 35s
    private var offlineTimeoutJob: Job? = null
    // null = not yet determined (show nothing), true = online, false = offline
    private val _sensorOnline = MutableLiveData<Boolean?>(null)
    val sensorOnline: LiveData<Boolean?> = _sensorOnline

    private fun resetOfflineTimer() {
        offlineTimeoutJob?.cancel()
        offlineTimeoutJob = viewModelScope.launch {
            delay(35_000)
            _sensorOnline.postValue(false)
            android.util.Log.w("RTDB", "No update in 35s — sensor marked OFFLINE")
        }
    }

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
                    lastUpdate = when (val raw = doc.get("lastUpdate")) {
                        is com.google.firebase.Timestamp -> raw.toDate().time
                        is Long -> raw
                        is Number -> raw.toLong()
                        else -> 0L
                    }
                )
                _apiaryData.value = apiary

                // Set the initial online state from Firestore immediately —
                // this prevents the brief "Offline" flash while RTDB connects.
                // The RTDB listener will confirm/correct this once it fires.
                if (_sensorOnline.value != apiary.isConnected) {
                    _sensorOnline.postValue(apiary.isConnected)
                }
                val nodeId = apiary.nodeId
                if (nodeId.isNotBlank()) {
                    startRtdbListener(nodeId, apiaryId)
                }

                if (apiary.isConnected && apiary.weight > 0) {
                    saveWeightReading(apiaryId, apiary.weight)
                }
            }

        fetchWeightAnalytics(apiaryId)
        listenWeightHistory(apiaryId)
    }

    private fun startRtdbListener(nodeId: String, apiaryId: String) {
        val path = "/$nodeId"
        if (rtdbNodePath == path) return

        rtdbListener?.let {
            rtdbNodePath?.let { oldPath -> rtdb.getReference(oldPath).removeEventListener(it) }
        }
        rtdbNodePath = path
        offlineTimeoutJob?.cancel()

        rtdb.getReference(path).keepSynced(false)

        android.util.Log.d("RTDB", "Starting listener on path: $path for apiary: $apiaryId")

        // Do NOT reset sensorOnline here — Firestore already set the correct
        // initial state in startListening. Only RTDB callbacks update it from here.

        // Track whether the first onDataChange has fired (always a cached replay)
        var firstCallbackDone = false

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    android.util.Log.w("RTDB", "No data at path: $path")
                    _sensorOnline.postValue(false)
                    return
                }

                val espTimestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val nowMs = System.currentTimeMillis()

                if (espTimestamp > 0L) {
                    // ESP32 writes a timestamp — definitive freshness check
                    val ageMs = nowMs - espTimestamp
                    if (ageMs > 60_000L) {
                        android.util.Log.w("RTDB", "Stale ESP32 timestamp ${ageMs}ms — offline")
                        _sensorOnline.postValue(false)
                        return
                    }
                    android.util.Log.d("RTDB", "Fresh ESP32 timestamp ${ageMs}ms — online")
                    updateSensorValues(snapshot)
                    _sensorOnline.postValue(true)
                    resetOfflineTimer()
                    return
                }

                // No ESP32 timestamp — skip the very first callback (cached replay)
                if (!firstCallbackDone) {
                    firstCallbackDone = true
                    android.util.Log.d("RTDB", "First callback (cached replay) — skipping, keeping Firestore state")
                    // Do NOT override sensorOnline here — Firestore already set the correct
                    // initial state. Overriding with false causes the "Offline" flash when
                    // the device is actually online.
                    return
                }

                // Second+ callback = live data from ESP32
                android.util.Log.d("RTDB", "Live update (no timestamp) — marking online")
                updateSensorValues(snapshot)
                _sensorOnline.postValue(true)
                resetOfflineTimer()
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("RTDB", "Listener cancelled: ${error.message}")
                _errorMessage.postValue("Sensor connection error: ${error.message}")
            }
        }

        rtdb.getReference(path).addValueEventListener(listener)
        rtdbListener = listener
    }

    private fun updateSensorValues(snapshot: DataSnapshot) {
        val temperature    = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
        val humidity       = snapshot.child("humidity").getValue(Double::class.java)    ?: 0.0
        val lidOpen        = snapshot.child("lidOpen").getValue(Boolean::class.java)    ?: false
        val weightKg       = snapshot.child("weight").getValue(Double::class.java)      ?: 0.0
        val moistureForLid = if (lidOpen) 10.0 else 0.0
        android.util.Log.d("RTDB", "Sensor values → T=$temperature H=$humidity Lid=${if (lidOpen) "OPEN" else "CLOSED"} W=$weightKg")
        // Update the local LiveData so the UI reflects latest values
        _apiaryData.value = _apiaryData.value?.copy(
            temperature = temperature,
            humidity = humidity,
            moisture = moistureForLid,
            weight = weightKg
        )
    }

    private fun saveWeightReading(apiaryId: String, weight: Double) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
            val label = sdf.format(java.util.Date())
            firestore.collection("apiaries").document(apiaryId)
                .collection("weight_history")
                .add(mapOf(
                    "weight"    to weight,
                    "label"     to label,
                    "timestamp" to FieldValue.serverTimestamp()
                ))
        }
    }

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
                    .reversed()
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
        rtdbListener?.let {
            rtdbNodePath?.let { path -> rtdb.getReference(path).removeEventListener(it) }
        }
        rtdbListener = null
        rtdbNodePath = null
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
        analyticsListener?.remove()
        historyListener?.remove()
        rtdbListener?.let {
            rtdbNodePath?.let { path -> rtdb.getReference(path).removeEventListener(it) }
        }
    }
}
