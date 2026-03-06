package com.example.agrihive.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.UUID

class ActivityLogViewModel : ViewModel() {

    companion object {
        @Volatile
        private var instance: ActivityLogViewModel? = null

        fun getInstance(): ActivityLogViewModel {
            return instance ?: synchronized(this) {
                instance ?: ActivityLogViewModel().also { instance = it }
            }
        }
    }

    private val repository = ActivityLogRepository.getInstance()

    private val _activityLogs = MutableLiveData<List<ActivityLogItem>>()
    val activityLogs: LiveData<List<ActivityLogItem>> = _activityLogs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Track if we're currently loading from Firebase
    @Volatile
    private var isLoadingFromFirebase = false

    // Track current user ID to detect logout/login
    private var currentUserId: String? = null

    // Store logs in memory for persistence
    private val cachedLogs = mutableListOf<ActivityLogItem>()

    init {
        _activityLogs.value = emptyList()
    }

    private fun loadActivityLogs() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        // If no user logged in, don't load
        if (uid == null) {
            _isLoading.value = false
            _activityLogs.value = emptyList()
            return
        }

        // Check if user changed - if so, clear cached data and load fresh
        if (currentUserId != null && currentUserId != uid) {
            // User switched accounts - clear old data
            cachedLogs.clear()
            _activityLogs.value = emptyList()
        }
        currentUserId = uid

        _isLoading.value = true
        isLoadingFromFirebase = true

        repository.getActivityLogs(
            onSuccess = { logs ->
                _isLoading.value = false
                isLoadingFromFirebase = false
                
                // Update cache with fresh Firebase data
                cachedLogs.clear()
                cachedLogs.addAll(logs)
                
                // Update LiveData
                _activityLogs.value = logs.sortedByDescending { it.timestamp }
            },
            onFailure = { exception ->
                _isLoading.value = false
                isLoadingFromFirebase = false
                
                // Show cached data on failure
                _activityLogs.value = cachedLogs.sortedByDescending { it.timestamp }
                _errorMessage.value = exception.message
            }
        )
    }

    /**
     * Force refresh - reload from Firebase
     */
    fun refresh() {
        isLoadingFromFirebase = false
        loadActivityLogs()
    }

    /**
     * Clear all cached data.
     * Call this when user logs out.
     */
    fun clearCache() {
        repository.clearLocalLogs()
        cachedLogs.clear()
        _activityLogs.value = emptyList()
        currentUserId = null
        isLoadingFromFirebase = false
    }

    /**
     * Load from Firebase - should be called when activity becomes visible
     * This ensures we always get the latest user-specific data
     */
    fun loadFromFirebase() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        
        // If user changed (different account), clear everything and reload
        if (currentUserId != currentUid) {
            cachedLogs.clear()
            _activityLogs.value = emptyList()
            repository.clearLocalLogs()
        }
        
        currentUserId = currentUid
        
        // Always force reload from Firebase
        isLoadingFromFirebase = false
        loadActivityLogs()
    }

    /**
     * Add a new activity log entry
     */
    fun addLog(type: LogType, description: String, userName: String? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            return // Can't log without user
        }
        
        // Create new log entry
        val newLog = ActivityLogItem(
            id = UUID.randomUUID().toString(),
            type = type,
            title = description,
            description = description,
            timestamp = Calendar.getInstance().time,
            userName = userName
        )
        
        // Add to cache immediately for instant display
        cachedLogs.add(0, newLog)
        
        // Update LiveData
        val currentLogs = _activityLogs.value?.toMutableList() ?: mutableListOf()
        currentLogs.add(0, newLog)
        _activityLogs.value = currentLogs.sortedByDescending { it.timestamp }
        
        // Save to Firebase in background
        repository.saveActivityLog(
            type = type,
            title = description,
            description = description
        )
    }

    fun logPasswordChanged() {
        addLog(LogType.USER_ACCOUNT, "Password Changed", "You")
    }

    fun logProfileUpdated(field: String) {
        addLog(LogType.USER_ACCOUNT, "Profile Updated: $field", "You")
    }

    fun logTemperatureAlert(hiveName: String, temperature: Float, threshold: Float) {
        addLog(
            LogType.HIVE_SENSOR,
            "$hiveName - High temperature alert: ${temperature.toInt()}°C exceeds threshold of ${threshold.toInt()}°C"
        )
    }

    fun logFanActivated(hiveName: String) {
        addLog(
            LogType.HIVE_SENSOR,
            "$hiveName - Cooling fan automatically activated"
        )
    }

    fun logSubscription(planName: String, price: Double, paymentMethod: String) {
        addLog(
            LogType.SUBSCRIPTION,
            "Subscribed to $planName - P${String.format("%.2f", price)} via $paymentMethod"
        )
    }

    fun clearLogs() {
        cachedLogs.clear()
        _activityLogs.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
