package com.example.agrihive.log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar
import java.util.UUID

class ActivityLogViewModel : ViewModel() {

    private val _activityLogs = MutableLiveData<List<ActivityLogItem>>()
    val activityLogs: LiveData<List<ActivityLogItem>> = _activityLogs

    init {
        loadSampleLogs()
    }

    private fun loadSampleLogs() {
        val logs = mutableListOf<ActivityLogItem>()
        val calendar = Calendar.getInstance()

        // Sample Hive Sensor Events
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.MINUTE, -5)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.HIVE_SENSOR,
                title = "Temperature Normalized",
                description = "Hive 1 - Temperature Normalized (35°C)",
                timestamp = calendar.time,
                userName = "System"
            )
        )

        calendar.add(Calendar.MINUTE, -15)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.HIVE_SENSOR,
                title = "High Temperature",
                description = "Hive 1 - High Temperature (38.2°C) → Fan automatically turned On",
                timestamp = calendar.time,
                userName = "System"
            )
        )

        // Sample User Account Actions
        calendar.add(Calendar.HOUR, -2)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.USER_ACCOUNT,
                title = "Profile Update",
                description = "James Collin changed his profile picture",
                timestamp = calendar.time,
                userName = "James Collin"
            )
        )

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.USER_ACCOUNT,
                title = "Password Changed",
                description = "James Collin changed his password",
                timestamp = calendar.time,
                userName = "James Collin"
            )
        )

        // Sample Data Actions
        calendar.add(Calendar.HOUR, -3)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.DATA_ACTION,
                title = "Result Saved",
                description = "James Collin saved result treatment",
                timestamp = calendar.time,
                userName = "James Collin"
            )
        )

        // Sample Subscription Events
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.SUBSCRIPTION,
                title = "Payment",
                description = "Paid Subscription - P899.00",
                timestamp = calendar.time,
                userName = "James Collin"
            )
        )

        // Sample System Events
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.SYSTEM,
                title = "Notification Enabled",
                description = "Notification enabled",
                timestamp = calendar.time,
                userName = "James Collin"
            )
        )

        calendar.add(Calendar.DAY_OF_YEAR, -5)
        logs.add(
            ActivityLogItem(
                id = UUID.randomUUID().toString(),
                type = LogType.SYSTEM,
                title = "Settings Changed",
                description = "Fan Auto-Control time set to 1 minute",
                timestamp = calendar.time,
                userName = "James Collin"
            )
        )

        _activityLogs.value = logs.sortedByDescending { it.timestamp }
    }

    fun addLog(type: LogType, description: String, userName: String? = null) {
        val currentLogs = _activityLogs.value?.toMutableList() ?: mutableListOf()
        val newLog = ActivityLogItem(
            id = UUID.randomUUID().toString(),
            type = type,
            title = description,
            description = description,
            timestamp = Calendar.getInstance().time,
            userName = userName
        )
        currentLogs.add(0, newLog)
        _activityLogs.value = currentLogs
    }

    fun clearLogs() {
        _activityLogs.value = emptyList()
    }
}
