package com.example.agrihive.log

import java.util.Date

/**
 * Represents an activity log entry in the app.
 * Types: HIVE_SENSOR, USER_ACCOUNT, DATA_ACTION, SUBSCRIPTION, SYSTEM
 */
data class ActivityLogItem(
    val id: String,
    val type: LogType,
    val title: String,
    val description: String,
    val timestamp: Date,
    val userName: String? = null
)

enum class LogType {
    HIVE_SENSOR,    // Temperature thresholds, fan auto-triggers
    USER_ACCOUNT,   // Profile changes, password changes
    DATA_ACTION,    // Save results, capture photos
    SUBSCRIPTION,    // Payment activity
    SYSTEM           // Setting changes, notifications
}
