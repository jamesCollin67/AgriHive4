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
    private val _sensorOnline = MutableLiveData<Boolean>(false)
    val sensorOnline: LiveData<Boolean> = _sensorOnline

    // Tracks whether we have received at least one server-confirmed update this session
    private var receivedLiveUpdate = false

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
        receivedLiveUpdate = false

        // Disable local cache for this node so we never get stale cached data
        rtdb.getReference(path).keepSynced(false)

        android.util.Log.d("RTDB", "Starting listener on path: $path for apiary: $apiaryId")

        // Start offline — only flip to online when a server-confirmed update arrives
        _sensorOnline.postValue(false)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    android.util.Log.w("RTDB", "No data at path: $path")
                    _sensorOnline.postValue(false)
                    return
                }

                // Use the ESP32 timestamp field to distinguish live vs cached data.
                // If the ESP32 doesn't write a timestamp, fall back to lastUpdate from Firestore.
                val espTimestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                val nowMs = System.currentTimeMillis()

                if (espTimestamp > 0L) {
                    val ageMs = nowMs - espTimestamp
                    if (ageMs > 60_000L) {
                        android.util.Log.w("RTDB", "Stale data age=${ageMs}ms — offline")
                        _sensorOnline.postValue(false)
                        return
                    }
                    // Fresh confirmed by ESP32 timestamp
                    markOnlineAndSync(snapshot, apiaryId)
                } else {
                    // No ESP32 timestamp — the very first onDataChange is always a cached
                    // replay from Firebase's in-memory store. Only trust it if we have
                    // already received at least one live update this session.
                    if (!receivedLiveUpdate) {
                        android.util.Log.w("RTDB", "First onDataChange, no timestamp — treating as stale cache, staying offline")
                        _sensorOnline.postValue(false)
                        return
                    }
                    markOnlineAndSync(snapshot, apiaryId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("RTDB", "Listener cancelled: ${error.message}")
                _errorMessage.postValue("Sensor connection error: ${error.message}")
            }
        }

        rtdb.getReference(path).addValueEventListener(listener)
        rtdbListener = listener
    }

    private fun markOnlineAndSync(snapshot: DataSnapshot, apiaryId: String) {
        receivedLiveUpdate = true
        _sensorOnline.postValue(true)
        resetOfflineTimer()

        val temperature    = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
        val humidity       = snapshot.child("humidity").getValue(Double::class.java)    ?: 0.0
        val lidOpen        = snapshot.child("lidOpen").getValue(Boolean::class.java)    ?: false
        val weightKg       = snapshot.child("weight").getValue(Double::class.java)      ?: 0.0
        val moistureForLid = if (lidOpen) 10.0 else 0.0

        android.util.Log.d("RTDB", "Live data → T=$temperature H=$humidity Lid=${if (lidOpen) "OPEN" else "CLOSED"} W=$weightKg")

        firestore.collection("apiaries").document(apiaryId)
            .update(mapOf(
                "temperature" to temperature,
                "humidity"    to humidity,
                "moisture"    to moistureForLid,
                "weight"      to weightKg,
                "isConnected" to true,
                "lastUpdate"  to FieldValue.serverTimestamp()
            ))
            .addOnFailureListener { e ->
                android.util.Log.e("RTDB", "Firestore update failed: ${e.message}")
            }
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
