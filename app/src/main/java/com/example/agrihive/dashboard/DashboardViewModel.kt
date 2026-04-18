package com.example.agrihive.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.agrihive.addapiary.Apiary
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.data.local.AgriHiveDatabase
import com.example.agrihive.data.local.ApiaryEntity
import com.example.agrihive.weather.RainAlertNotification
import com.example.agrihive.utils.NetworkConnectivityObserver
import com.example.agrihive.utils.ConnectivityObserver
import com.example.agrihive.notification.NotificationItem
import com.example.agrihive.notification.NotificationRepository
import com.example.agrihive.notification.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
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

    private val _unreadNotificationsCount = MutableLiveData<Int>(0)
    val unreadNotificationsCount: LiveData<Int> = _unreadNotificationsCount

    private val _connectionStatus = MutableLiveData<ConnectivityObserver.Status>()
    val connectionStatus: LiveData<ConnectivityObserver.Status> = _connectionStatus

    private val _subscriptionExpired = MutableLiveData<Boolean>()
    val subscriptionExpired: LiveData<Boolean> = _subscriptionExpired

    private var apiaryListener: ListenerRegistration? = null
    private var reportReplyListener: ListenerRegistration? = null

    // RTDB listeners — one per apiary nodeId, kept alive on the dashboard
    private val rtdb = FirebaseDatabase.getInstance()
    private val rtdbListeners = mutableMapOf<String, ValueEventListener>() // nodeId → listener
    private val offlineJobs   = mutableMapOf<String, kotlinx.coroutines.Job>() // nodeId → timeout job

    init {
        observeConnection()
        loadUserName()
        loadFromCache()
        updateFcmToken()
        startReportReplyListener()
        updateNotificationCount()
        checkSubscriptionStatus()
    }

    fun updateNotificationCount() {
        val repository = NotificationRepository(getApplication())
        _unreadNotificationsCount.postValue(repository.getUnreadCount())
    }

    private fun startReportReplyListener() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        Log.d("ReportReply", "Starting listener for user: $uid")
        
        reportReplyListener?.remove()
        reportReplyListener = firestore.collection("reports")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ReportReply", "Listener error", e)
                    return@addSnapshotListener
                }

                Log.d("ReportReply", "Snapshot received with ${snapshot?.size()} documents")

                snapshot?.documentChanges?.forEach { change ->
                    val doc = change.document
                    val data = doc.data
                    val reply = data["reply"] as? String
                    // If notified is missing, we assume false to ensure we catch it
                    val notified = data["notified"] as? Boolean ?: false

                    Log.d("ReportReply", "Doc ${doc.id}: notified=$notified, hasReply=${!reply.isNullOrBlank()}")

                    // If there's a reply and it hasn't been notified yet
                    if (!reply.isNullOrBlank() && !notified) {
                        Log.d("ReportReply", "New reply found! Triggering notification...")
                        
                        // 1. Mark as notified in Firestore IMMEDIATELY to prevent loops
                        firestore.collection("reports").document(doc.id)
                            .update("notified", true)
                            .addOnSuccessListener { Log.d("ReportReply", "Marked as notified in Firestore") }
                            .addOnFailureListener { Log.e("ReportReply", "Failed to mark notified", it) }

                        // 2. Show system notification (saves to repository and shows popup)
                        RainAlertNotification.showAdminReplyNotification(getApplication(), reply)
                        
                        // 3. Update count for UI badge
                        updateNotificationCount()
                    }
                }
            }
    }

    private fun updateFcmToken() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                firestore.collection("users").document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token updated on dashboard: $token")
                    }
            }
        }
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
                        // Safely handle both Timestamp and Long — getTimestamp() throws if field is a Long
                        lastUpdate = when (val raw = doc.get("lastUpdate")) {
                            is com.google.firebase.Timestamp -> raw.toDate().time
                            is Long -> raw
                            is Number -> raw.toLong()
                            else -> 0L
                        }
                    )
                } ?: emptyList()

                _apiaries.value = list
                calculateStats(list)
                saveToCache(list)

                // Start real-time RTDB sync for every apiary that has a nodeId
                // This keeps dashboard cards live without needing to open HiveStreams
                syncRtdbForApiaries(list)
            }
    }

    /**
     * For each apiary with a nodeId, attach an RTDB listener that pushes
     * live sensor data into Firestore. This makes dashboard cards update
     * in real-time without the user needing to open HiveStreams first.
     */
    private fun syncRtdbForApiaries(apiaries: List<Apiary>) {
        val activeNodeIds = apiaries.mapNotNull { it.nodeId.takeIf { id -> id.isNotBlank() } }.toSet()

        // Remove listeners for nodeIds no longer in the list
        val toRemove = rtdbListeners.keys.filter { it !in activeNodeIds }
        toRemove.forEach { nodeId ->
            rtdbListeners[nodeId]?.let { rtdb.getReference("/$nodeId").removeEventListener(it) }
            rtdbListeners.remove(nodeId)
            Log.d("RTDB", "Removed listener for nodeId: $nodeId")
        }

        // Add listeners for new nodeIds
        apiaries.forEach { apiary ->
            val nodeId = apiary.nodeId
            if (nodeId.isBlank() || rtdbListeners.containsKey(nodeId)) return@forEach

            val apiaryId = apiary.id
            Log.d("RTDB", "Dashboard: attaching RTDB listener → /$nodeId → apiary $apiaryId")

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
                    val humidity    = snapshot.child("humidity").getValue(Double::class.java)    ?: 0.0
                    val lidOpen     = snapshot.child("lidOpen").getValue(Boolean::class.java)    ?: false
                    val weightKg    = snapshot.child("weight").getValue(Double::class.java)      ?: 0.0

                    Log.d("RTDB", "[$nodeId] T=$temperature H=$humidity Lid=${if (lidOpen) "OPEN" else "CLOSED"} W=$weightKg")

                    val moistureForLid = if (lidOpen) 10.0 else 0.0

                    // Data arrived — sensor is online, reset the 30s offline timer
                    offlineJobs[nodeId]?.cancel()
                    offlineJobs[nodeId] = viewModelScope.launch {
                        // Mark online immediately
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
                                Log.e("RTDB", "Firestore update failed for $nodeId: ${e.message}")
                            }

                        // After 30s with no new data, mark offline
                        kotlinx.coroutines.delay(30_000)
                        Log.w("RTDB", "[$nodeId] No update in 30s — marking OFFLINE")
                        firestore.collection("apiaries").document(apiaryId)
                            .update("isConnected", false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RTDB", "Listener cancelled for $nodeId: ${error.message}")
                }
            }

            rtdb.getReference("/$nodeId").addValueEventListener(listener)
            rtdbListeners[nodeId] = listener
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
        _alertsCount.value = list.count { apiary ->
            // Hive lid open (moisture = 10.0)
            (apiary.moisture >= 5.0 && apiary.isConnected) ||
            // Temperature out of range
            (apiary.temperature > 0 && (apiary.temperature < 34.0 || apiary.temperature > 36.0)) ||
            // Weight critically low
            (apiary.weight in 0.1..4.9)
        }
        _harvestReadyCount.value = list.count { it.weight > 5.0 && it.isConnected }
    }

    private fun ApiaryEntity.toDomain() = Apiary(
        id, name, location, nodeId, ownerId, temperature, humidity, moisture, weight, isConnected, alertsCount, lastUpdate
    )

    private fun Apiary.toEntity() = ApiaryEntity(
        id, name, location, nodeId, ownerId, temperature, humidity, moisture, weight, isConnected, alertsCount, lastUpdate
    )

    fun clearError() { _errorMessage.value = null }

    private fun checkSubscriptionStatus() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        // Delay check by 2 seconds so dashboard loads first
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            firestore.collection("subscriptions").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val due = doc.getString("due") ?: return@addOnSuccessListener
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val dueDate = sdf.parse(due)
                            if (dueDate != null && dueDate.before(java.util.Date())) {
                                _subscriptionExpired.postValue(true)
                            }
                        } catch (e: Exception) {
                            Log.e("Subscription", "Failed to parse due date: $due", e)
                        }
                    }
                    // No subscription document = user is on Basic plan, no expiry
                }
        }
    }

    fun clearSubscriptionExpired() {
        _subscriptionExpired.value = false
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
        reportReplyListener?.remove()
        // Remove all RTDB listeners and cancel offline timers
        rtdbListeners.forEach { (nodeId, listener) ->
            rtdb.getReference("/$nodeId").removeEventListener(listener)
        }
        rtdbListeners.clear()
        offlineJobs.values.forEach { it.cancel() }
        offlineJobs.clear()
    }
}
