package com.example.agrihive.notification

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository for managing app notifications
 * Stores notifications using SharedPreferences
 */
class NotificationRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "agrihive_notifications"
        private const val KEY_NOTIFICATIONS = "notifications_list"
        private const val MAX_NOTIFICATIONS = 50 // Keep last 50 notifications
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Add a new notification
     */
    fun addNotification(title: String, message: String, type: NotificationType): NotificationItem {
        val notification = NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        val notifications = getAllNotifications().toMutableList()
        notifications.add(0, notification) // Add to beginning

        // Keep only last MAX_NOTIFICATIONS
        val trimmedList = notifications.take(MAX_NOTIFICATIONS)
        saveNotifications(trimmedList)

        return notification
    }

    /**
     * Get all notifications
     */
    fun getAllNotifications(): List<NotificationItem> {
        val jsonString = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val notifications = mutableListOf<NotificationItem>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                notifications.add(
                    NotificationItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        type = NotificationType.valueOf(obj.getString("type")),
                        timestamp = obj.getLong("timestamp"),
                        isRead = obj.getBoolean("isRead")
                    )
                )
            }
            notifications
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get unread notifications count
     */
    fun getUnreadCount(): Int {
        return getAllNotifications().count { !it.isRead }
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: String) {
        val notifications = getAllNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveNotifications(notifications)
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        val notifications = getAllNotifications().map { it.copy(isRead = true) }
        saveNotifications(notifications)
    }

    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: String) {
        val notifications = getAllNotifications().filter { it.id != notificationId }
        saveNotifications(notifications)
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    /**
     * Save notifications to SharedPreferences
     */
    private fun saveNotifications(notifications: List<NotificationItem>) {
        val jsonArray = JSONArray()
        
        notifications.forEach { notification ->
            val obj = JSONObject().apply {
                put("id", notification.id)
                put("title", notification.title)
                put("message", notification.message)
                put("type", notification.type.name)
                put("timestamp", notification.timestamp)
                put("isRead", notification.isRead)
            }
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
    }
}
