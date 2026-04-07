package com.example.agrihive.notification

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository for managing app notifications
 * Stores notifications using SharedPreferences, partitioned by User ID
 */
class NotificationRepository(context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val appContext = context.applicationContext

    companion object {
        private const val PREFS_BASE_NAME = "agrihive_notifications"
        private const val KEY_NOTIFICATIONS = "notifications_list"
        private const val MAX_NOTIFICATIONS = 50 // Keep last 50 notifications
    }

    /**
     * Get user-specific SharedPreferences.
     * If no user is logged in, it returns a default one (though most ops require a user).
     */
    private val prefs: SharedPreferences
        get() {
            val uid = auth.currentUser?.uid ?: "anonymous"
            return appContext.getSharedPreferences("${PREFS_BASE_NAME}_$uid", Context.MODE_PRIVATE)
        }

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
     * Get all notifications for the CURRENT user
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
     * Get unread notifications count for the CURRENT user
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
     * Clear all notifications for the CURRENT user
     */
    fun clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    /**
     * Save notifications to user-specific SharedPreferences
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
