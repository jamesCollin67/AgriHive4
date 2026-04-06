package com.example.agrihive.notification

import java.io.Serializable

/**
 * Data model for app notifications
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) : Serializable

/**
 * Types of notifications in the app
 */
enum class NotificationType {
    RAIN_ALERT,       // Rain weather alerts
    FEEDING_ALERT,    // Feeding reminders
    TEMPERATURE_ALERT, // Temperature warnings
    SYSTEM,           // System notifications
    ADMIN_REPLY       // Admin reply to a beekeeper report
}
